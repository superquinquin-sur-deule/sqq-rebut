package org.superquinquin;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@QuarkusTestResource(WireMockOdooResource.class)
class ReleveResourceTest {

    @InjectWireMock
    WireMockServer wiremock;

    private int addLine(LocalDate dlc, int qty) {
        return given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "dlc", dlc.toString(), "qty", qty))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .extract().path("id");
    }

    @Test
    void addLineComputesUrgency() {
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "dlc", LocalDate.now().plusDays(2).toString(), "qty", 4))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("urgency", is("j2"))
                .body("qty", is(4))
                .body("name", is("FERME DU CRUSOBEAU Saucisse fine fromage"))
                .body("uom", is("kg"));
    }

    @Test
    void addLineMergesSameProductSameDlc() {
        int id1 = addLine(LocalDate.now().plusDays(7), 4);

        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "dlc", LocalDate.now().plusDays(7).toString(), "qty", 3))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("id", is(id1))
                .body("qty", is(7));

        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "dlc", LocalDate.now().plusDays(6).toString(), "qty", 2))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("id", not(id1));
    }

    @Test
    void updateQtyAndDelete() {
        int id = addLine(LocalDate.now().plusDays(1), 1);
        given().contentType(JSON).body(Map.of("qty", 5))
                .when().put("/api/releve/lines/" + id)
                .then().statusCode(200)
                .body("qty", is(5))
                .body("urgency", is("j1"));
        given().when().delete("/api/releve/lines/" + id)
                .then().statusCode(204);
    }

    @Test
    void addPerteLineUsesMotifLabelAndNoUrgency() {
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "type", "PERTE", "motifId", 8, "qty", 2))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("type", is("PERTE"))
                .body("motifLabel", is("Casse"))
                .body("urgency", nullValue())
                .body("dlc", nullValue())
                .body("qty", is(2));
    }

    @Test
    void addPerteMergesSameProductSameMotif() {
        int id1 = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "type", "PERTE", "motifId", 11, "qty", 2))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .extract().path("id");

        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "type", "PERTE", "motifId", 11, "qty", 3))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("id", is(id1))
                .body("qty", is(5));
    }

    @Test
    void addPerteWithoutMotifReturns400() {
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "PERTE", "qty", 1))
                .when().post("/api/releve/lines")
                .then().statusCode(400);
    }

    @Test
    void rebutPerteScrapsWithChosenMotifOrigin() {
        int id = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "type", "PERTE", "motifId", 9, "qty", 1))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .extract().path("id");

        given().contentType(JSON).body(Map.of("lineIds", java.util.List.of(id)))
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("created", greaterThanOrEqualTo(1))
                .body("lines[0].sent", is(true));

        // le scrap part avec l'origine = le motif choisi (9), pas la config DLC (7)
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"scrap_origin_id\":9")));
    }

    @Test
    void rebutJ0CreatesAndValidatesScrapWithBasicAuth() {
        addLine(LocalDate.now(), 3); // j0

        given().contentType(JSON).body("{}")
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("dryRun", is(false))
                .body("created", greaterThanOrEqualTo(1))
                .body("lines[0].sent", is(true));

        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withHeader("Authorization", containing("Basic")));
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"create\"")));
    }
}
