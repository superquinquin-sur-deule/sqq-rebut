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
                .body("qty", is(4.0f))
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
                .body("qty", is(7.0f));

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
                .body("qty", is(5.0f))
                .body("urgency", is("j1"));
        given().contentType(JSON).body(Map.of("qty", 0))
                .when().put("/api/releve/lines/" + id)
                .then().statusCode(400);
        given().contentType(JSON).body(Map.of("qty", 0.5))
                .when().put("/api/releve/lines/" + id)
                .then().statusCode(200)
                .body("qty", is(0.5f));
        given().when().delete("/api/releve/lines/" + id)
                .then().statusCode(204);
    }

    @Test
    void addPerteLineScrapsImmediatelyWithChosenMotif() {
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "type", "PERTE", "motifId", 8, "qty", 2))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("type", is("PERTE"))
                .body("motifLabel", is("Casse"))
                .body("urgency", nullValue())
                .body("dlc", nullValue())
                .body("qty", is(2.0f))
                .body("sent", is(true))
                .body("scrapRef", is("9999"));

        // le scrap part dès l'ajout, avec l'origine = le motif choisi (8)
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"scrap_origin_id\":8")));
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("action_validate")));
    }

    @Test
    void addPerteNeverMerges() {
        int id1 = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "type", "PERTE", "motifId", 11, "qty", 2))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("sent", is(true))
                .extract().path("id");

        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "type", "PERTE", "motifId", 11, "qty", 3))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("id", not(id1))
                .body("qty", is(3.0f))
                .body("sent", is(true));
    }

    @Test
    void updateOrDeleteSentLineReturns400() {
        int id = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "type", "PERTE", "motifId", 8, "qty", 2))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .extract().path("id");

        given().contentType(JSON).body(Map.of("qty", 5))
                .when().put("/api/releve/lines/" + id)
                .then().statusCode(400);
        given().when().delete("/api/releve/lines/" + id)
                .then().statusCode(400);
    }

    @Test
    void addLineWithScaleBarcodeStoresWeightAndMerges() {
        // scan balance → résolu vers le barcode de base, poids décimal
        int id1 = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.SCALE_BARCODE,
                        "dlc", LocalDate.now().plusDays(12).toString(), "qty", 1.234))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("barcode", is(WireMockOdooResource.KNOWN_BARCODE))
                .body("uom", is("kg"))
                .body("qty", is(1.234f))
                .extract().path("id");

        // second scan du même produit, autre poids, même DLC → fusion en sommant les kg
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "dlc", LocalDate.now().plusDays(12).toString(), "qty", 0.5))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("id", is(id1))
                .body("qty", is(1.734f));
    }

    @Test
    void addPerteWithoutMotifReturns400() {
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "PERTE", "qty", 1))
                .when().post("/api/releve/lines")
                .then().statusCode(400);
    }

    @Test
    void rebutIgnoresPerteLines() {
        int id = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "type", "PERTE", "motifId", 9, "qty", 1))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("sent", is(true))
                .extract().path("id");

        // la ligne est déjà partie au rebut à l'ajout : l'endpoint rebut ne la reprend pas
        given().contentType(JSON).body(Map.of("lineIds", java.util.List.of(id)))
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("created", is(0));
    }

    @Test
    void rebutJ0CreatesAndValidatesScrapWithBasicAuth() {
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "dlc", LocalDate.now().toString(), "qty", 1.234)) // j0, poids décimal
                .when().post("/api/releve/lines")
                .then().statusCode(200);

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
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"scrap_qty\":1.234")));
    }
}
