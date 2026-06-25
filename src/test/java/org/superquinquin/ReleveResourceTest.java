package org.superquinquin;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
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

    private int addPerte(String barcode) {
        return given().contentType(JSON)
                .body(Map.of("barcode", barcode, "type", "PERTE"))
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
    void addPerteLineDoesNotScrapAtAdd() {
        // Nouveau fonctionnement : on enchaîne les scans de pertes sans motif et sans envoi au
        // rebut. La ligne reste en attente (sent=false) jusqu'au rebut groupé.
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "PERTE"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("type", is("PERTE"))
                .body("motifLabel", nullValue())
                .body("urgency", nullValue())
                .body("dlc", nullValue())
                .body("sent", is(false))
                .body("scrapRef", nullValue());
    }

    @Test
    void addPerteMergesSameProduct() {
        // Rescanner le même produit cumule la quantité sur la même ligne (pas de doublon).
        // État DB partagé sur la classe : on raisonne en relatif par rapport à la 1re quantité.
        Response r1 = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "PERTE"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("sent", is(false))
                .extract().response();
        int id1 = r1.path("id");
        float q1 = r1.path("qty");

        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "PERTE"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("id", is(id1))
                .body("qty", is(q1 + 1.0f));
    }

    @Test
    void rebutScrapsPerteWithChosenMotif() {
        int id = addPerte(WireMockOdooResource.KNOWN_BARCODE);

        given().contentType(JSON).body(Map.of("lineIds", List.of(id), "motifId", 8))
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("created", is(1))
                .body("lines[0].sent", is(true))
                .body("lines[0].motifLabel", is("Casse"));

        // L'origine du scrap = le motif unique choisi au rebut (8 « Casse »).
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"scrap_origin_id\":8")));
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("action_validate")));
    }

    @Test
    void rebutForcesValidationWhenStockInsufficient() {
        // Fruits/légumes : le stock n'est pas saisi dans Odoo, donc action_validate renvoie le
        // wizard « quantité insuffisante » et laisse le scrap en brouillon. Le service doit forcer
        // la validation via do_scrap pour que la perte soit bien enregistrée.
        int id = addPerte(WireMockOdooResource.KNOWN_BARCODE);

        given().contentType(JSON)
                .body(Map.of("lineIds", List.of(id), "motifId", WireMockOdooResource.INSUFFICIENT_STOCK_ORIGIN_ID))
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("created", is(1))
                .body("lines[0].sent", is(true))
                .body("lines[0].scrapRef", is(String.valueOf(WireMockOdooResource.INSUFFICIENT_STOCK_SCRAP_ID)));

        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("do_scrap"))
                .withRequestBody(containing(String.valueOf(WireMockOdooResource.INSUFFICIENT_STOCK_SCRAP_ID))));
    }

    @Test
    void rebutDoesNotForceWhenStockSufficient() {
        // Stock suffisant : action_validate renvoie true, aucun do_scrap ne doit être émis.
        // Le journal WireMock est partagé sur toute la classe : on le réinitialise juste avant le
        // rebut ciblé pour pouvoir affirmer « zéro do_scrap » sans compter ceux des autres tests.
        int id = addPerte(WireMockOdooResource.KNOWN_BARCODE);

        wiremock.resetRequests();
        given().contentType(JSON).body(Map.of("lineIds", List.of(id), "motifId", 8))
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("created", is(1))
                .body("lines[0].sent", is(true))
                .body("lines[0].scrapRef", is("9999"));

        wiremock.verify(0, postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("do_scrap")));
    }

    @Test
    void updateOrDeleteSentLineReturns400() {
        int id = addPerte(WireMockOdooResource.KNOWN_BARCODE);
        given().contentType(JSON).body(Map.of("lineIds", List.of(id), "motifId", 8))
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("lines[0].sent", is(true));

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
    void rebutWithoutMotifReturns400() {
        // Le motif est désormais obligatoire et unique pour toute la liste, choisi au rebut.
        int id = addPerte(WireMockOdooResource.KNOWN_BARCODE);
        given().contentType(JSON).body(Map.of("lineIds", List.of(id)))
                .when().post("/api/releve/rebut")
                .then().statusCode(400);
    }

    @Test
    void rebutPerteByProductIdWithoutBarcode() {
        int id = given().contentType(JSON)
                .body(Map.of("productId", WireMockOdooResource.NO_BARCODE_PRODUCT_ID, "type", "PERTE"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("type", is("PERTE"))
                .body("barcode", nullValue())
                .body("sent", is(false))
                .extract().path("id");

        given().contentType(JSON).body(Map.of("lineIds", List.of(id), "motifId", 8))
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("lines[0].sent", is(true))
                .body("lines[0].scrapRef", is("9999"));

        // le scrap est créé par product_id, sans code-barres
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"product_id\":" + WireMockOdooResource.NO_BARCODE_PRODUCT_ID)));
    }

    @Test
    void addReassortLineWithoutQtyNeverScraps() {
        // Le réassort est une simple liste de produits : pas de quantité dans la requête.
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "REASSORT"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("type", is("REASSORT"))
                .body("urgency", nullValue())
                .body("dlc", nullValue())
                .body("motifLabel", nullValue())
                .body("qty", is(1.0f)) // qté fixée à 1 en interne (colonne non-null), jamais affichée
                .body("qtyAvailable", is(2.248f)) // stock Odoo figé au scan, affiché sur la ligne réassort
                .body("sent", is(false)) // jamais envoyé au rebut : le réassort est une liste interne
                .body("scrapRef", nullValue());
    }

    @Test
    void addReassortSameProductIsNoOp() {
        int id1 = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "REASSORT"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .extract().path("id");

        // Re-scanner le même produit ne crée pas de doublon et ne cumule aucune quantité.
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "REASSORT"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("id", is(id1))
                .body("qty", is(1.0f));
    }

    @Test
    void deleteReassortLine() {
        int id = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "REASSORT"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .extract().path("id");
        given().when().delete("/api/releve/lines/" + id)
                .then().statusCode(204);
    }

    @Test
    void rebutIgnoresReassortLines() {
        int id = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "REASSORT"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .extract().path("id");

        // Même avec un motif, le rebut ne touche que les pertes : le réassort est ignoré.
        given().contentType(JSON).body(Map.of("lineIds", List.of(id), "motifId", 8))
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("created", is(0));
    }

    @Test
    void addLineByProductIdWithoutBarcode() {
        given().contentType(JSON)
                .body(Map.of("productId", WireMockOdooResource.NO_BARCODE_PRODUCT_ID,
                        "dlc", LocalDate.now().plusDays(2).toString(), "qty", 1))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("name", is("Salade verte"))
                .body("barcode", nullValue())
                .body("urgency", is("j2"))
                .body("qty", is(1.0f));
    }

    @Test
    void addLineByProductIdDedupsByProductId() {
        int id1 = given().contentType(JSON)
                .body(Map.of("productId", WireMockOdooResource.NO_BARCODE_PRODUCT_ID,
                        "dlc", LocalDate.now().plusDays(3).toString(), "qty", 2))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .extract().path("id");

        given().contentType(JSON)
                .body(Map.of("productId", WireMockOdooResource.NO_BARCODE_PRODUCT_ID,
                        "dlc", LocalDate.now().plusDays(3).toString(), "qty", 3))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("id", is(id1))
                .body("qty", is(5.0f));
    }

    @Test
    void addLineWithoutBarcodeNorProductIdReturns400() {
        given().contentType(JSON)
                .body(Map.of("dlc", LocalDate.now().plusDays(1).toString(), "qty", 1))
                .when().post("/api/releve/lines")
                .then().statusCode(400);
    }

    @Test
    void rebutScrapsPerteWithBasicAuth() {
        // Purge d'éventuelles pertes laissées par d'autres tests (état DB partagé dans la classe),
        // pour que la ligne ajoutée ensuite soit neuve et porte exactement le poids décimal testé.
        given().contentType(JSON).body(Map.of("motifId", 8))
                .when().post("/api/releve/rebut").then().statusCode(200);

        int id = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "PERTE", "qty", 1.234))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("qty", is(1.234f))
                .extract().path("id");

        wiremock.resetRequests();
        given().contentType(JSON).body(Map.of("lineIds", List.of(id), "motifId", 8))
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
