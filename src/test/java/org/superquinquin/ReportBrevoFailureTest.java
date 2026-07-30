package org.superquinquin;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.superquinquin.report.ReportSettingsService;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/** Clé API refusée par Brevo : l'erreur doit remonter en 502 et être tracée dans les réglages. */
@QuarkusTest
@QuarkusTestResource(WireMockOdooResource.class)
@TestProfile(ReportBrevoFailureTest.InvalidKeyProfile.class)
class ReportBrevoFailureTest {

    public static class InvalidKeyProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("brevo.api-key", WireMockOdooResource.INVALID_BREVO_KEY);
        }
    }

    @Test
    void sendNowSurfacesBrevoErrors() {
        given().contentType(JSON)
                .body(Map.of("enabled", false, "sendTime", "23:59",
                        "recipients", List.of("resp@sqq.fr"),
                        "thresholdPieces", 5, "thresholdKg", 1))
                .when().put("/api/report/settings")
                .then().statusCode(200);

        given().contentType(JSON)
                .when().post("/api/report/send-now")
                .then().statusCode(502)
                .body("error", is("brevo"))
                .body("message", containsString("401"));

        given().when().get("/api/report/settings")
                .then().statusCode(200)
                .body("lastStatus", is(ReportSettingsService.STATUS_ERROR));
    }
}
