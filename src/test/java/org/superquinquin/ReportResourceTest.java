package org.superquinquin;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.superquinquin.report.ReportSettingsService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(WireMockOdooResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReportResourceTest {

    private static final String RECIPIENT = "resp@sqq.fr";
    /** Heure éloignée : les réglages activés ne doivent pas déclencher d'envoi pendant les tests. */
    private static final String LATE = "23:59";

    @InjectWireMock
    WireMockServer wiremock;

    @Inject
    ReportSettingsService settings;

    private static Map<String, Object> body(double pieces, double kg) {
        return Map.of("enabled", true, "sendTime", LATE, "recipients", List.of(RECIPIENT),
                "thresholdPieces", pieces, "thresholdKg", kg);
    }

    private static void save(Map<String, Object> payload) {
        given().contentType(JSON).body(payload)
                .when().put("/api/report/settings")
                .then().statusCode(200);
    }

    private static void addDlcLine(Object productKey, LocalDate dlc, double qty) {
        Map<String, Object> payload = productKey instanceof Long id
                ? Map.of("productId", id, "dlc", dlc.toString(), "qty", qty)
                : Map.of("barcode", productKey, "dlc", dlc.toString(), "qty", qty);
        given().contentType(JSON).body(payload)
                .when().post("/api/releve/lines")
                .then().statusCode(200);
    }

    @Test
    @Order(1)
    void defaultsComeFromTheMigration() {
        given().when().get("/api/report/settings")
                .then().statusCode(200)
                .body("enabled", is(false))
                .body("sendTime", is("18:00"))
                .body("recipients", is(empty()))
                .body("thresholdPieces", is(5.0f))
                .body("thresholdKg", is(1.0f));
    }

    @Test
    @Order(2)
    void savesAndReadsBackSettings() {
        given().contentType(JSON)
                .body(Map.of("enabled", true, "sendTime", LATE,
                        "recipients", List.of("  Resp@Sqq.fr ", "resp@sqq.fr"),
                        "thresholdPieces", 4, "thresholdKg", 0.5))
                .when().put("/api/report/settings")
                .then().statusCode(200)
                // adresses normalisées et dédoublonnées
                .body("recipients", is(List.of(RECIPIENT)));

        given().when().get("/api/report/settings")
                .then().statusCode(200)
                .body("enabled", is(true))
                .body("sendTime", is(LATE))
                .body("thresholdPieces", is(4.0f))
                .body("thresholdKg", is(0.5f));
    }

    @Test
    @Order(3)
    void rejectsInvalidSettings() {
        given().contentType(JSON)
                .body(Map.of("enabled", false, "sendTime", "25:00", "recipients", List.of(),
                        "thresholdPieces", 5, "thresholdKg", 1))
                .when().put("/api/report/settings").then().statusCode(400);

        given().contentType(JSON)
                .body(Map.of("enabled", false, "sendTime", LATE, "recipients", List.of("pas-un-email"),
                        "thresholdPieces", 5, "thresholdKg", 1))
                .when().put("/api/report/settings").then().statusCode(400);

        given().contentType(JSON)
                .body(Map.of("enabled", false, "sendTime", LATE, "recipients", List.of(),
                        "thresholdPieces", -1, "thresholdKg", 1))
                .when().put("/api/report/settings").then().statusCode(400);

        // Activer l'envoi sans destinataire n'a pas de sens.
        given().contentType(JSON)
                .body(Map.of("enabled", true, "sendTime", LATE, "recipients", List.of(),
                        "thresholdPieces", 5, "thresholdKg", 1))
                .when().put("/api/report/settings").then().statusCode(400);
    }

    @Test
    @Order(4)
    void sendNowMailsTheGroupedReportWithItsWorkbook() {
        save(body(5, 1));
        LocalDate today = LocalDate.now();
        // 2,5 kg à J-0 (> 1 kg) et 8 pièces à J-1 (> 5 pièces) : les deux doivent figurer au rapport.
        addDlcLine(WireMockOdooResource.KNOWN_BARCODE, today, 2.5);
        addDlcLine(WireMockOdooResource.NO_BARCODE_PRODUCT_ID, today.plusDays(1), 8);

        wiremock.resetRequests();
        given().contentType(JSON)
                .when().post("/api/report/send-now")
                .then().statusCode(200)
                .body("lineCount", greaterThanOrEqualTo(2))
                .body("recipients", is(List.of(RECIPIENT)))
                .body("messageId", is(WireMockOdooResource.BREVO_MESSAGE_ID));

        wiremock.verify(postRequestedFor(urlEqualTo("/v3/smtp/email"))
                .withHeader("api-key", equalTo("cle-de-test"))
                .withRequestBody(matchingJsonPath("$.to[0].email", equalTo(RECIPIENT)))
                .withRequestBody(matchingJsonPath("$.sender.email",
                        equalTo("rebut-test@superquinquin.fr")))
                .withRequestBody(matchingJsonPath("$.attachment[0].name",
                        equalTo("rapport-dlc-" + today + ".xlsx")))
                .withRequestBody(matchingJsonPath("$.attachment[0].content", matching(".+")))
                .withRequestBody(containing("FERME DU CRUSOBEAU Saucisse fine fromage"))
                .withRequestBody(containing("Salade verte"))
                .withRequestBody(containing("J-0"))
                .withRequestBody(containing("J-1")));
    }

    @Test
    @Order(5)
    void emptyReportIsStillSentWithoutAttachment() {
        // Seuils inatteignables : plus aucune ligne ne passe le filtre.
        save(body(99_999, 99_999));

        wiremock.resetRequests();
        given().contentType(JSON)
                .when().post("/api/report/send-now")
                .then().statusCode(200)
                .body("lineCount", is(0));

        wiremock.verify(postRequestedFor(urlEqualTo("/v3/smtp/email"))
                .withRequestBody(containing("Rien à signaler"))
                // Un tableau « attachment » vide serait refusé par Brevo : la clé doit être absente.
                .withRequestBody(notMatching("(?s).*\"attachment\".*")));
    }

    @Test
    @Order(6)
    void lastSendIsReportedInSettings() {
        given().when().get("/api/report/settings")
                .then().statusCode(200)
                .body("lastStatus", is(ReportSettingsService.STATUS_OK));
    }

    @Test
    @Order(7)
    void scheduledSendIsClaimedOncePerDay() {
        LocalDate day = LocalDate.of(2000, 1, 1);
        assertTrue(settings.claimToday(day), "première réservation de la journée");
        assertFalse(settings.claimToday(day), "double envoi automatique le même jour");
    }
}
