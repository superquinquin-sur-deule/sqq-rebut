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
        // DLC dans 2 jours → j2
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
    void rebutJ0CreatesAndValidatesScrapWithBasicAuth() {
        addLine(LocalDate.now(), 3); // j0

        given().contentType(JSON).body("{}")
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("dryRun", is(false))
                .body("created", greaterThanOrEqualTo(1))
                .body("lines[0].sent", is(true));

        // l'en-tête Basic Auth (staging) a bien été émis vers Odoo
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withHeader("Authorization", containing("Basic")));
        // un create stock.scrap a bien eu lieu
        wiremock.verify(postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"create\"")));
    }
}
