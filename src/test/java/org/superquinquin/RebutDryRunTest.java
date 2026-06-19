package org.superquinquin;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@QuarkusTestResource(WireMockOdooResource.class)
@TestProfile(RebutDryRunTest.DryRunProfile.class)
class RebutDryRunTest {

    public static class DryRunProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("odoo.rebut.dry-run", "true");
        }
    }

    @InjectWireMock
    WireMockServer wiremock;

    @Test
    void rebutDryRunDoesNotWriteToOdoo() {
        int id = given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "PERTE"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .extract().path("id");

        given().contentType(JSON).body(Map.of("lineIds", List.of(id), "motifId", 8))
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("dryRun", is(true))
                .body("created", is(1))
                .body("lines[0].scrapRef", is("DRY-RUN"));

        // aucun create stock.scrap n'a été envoyé à Odoo
        wiremock.verify(0, postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"create\"")));
    }

    @Test
    void addPerteDryRunDoesNotWriteToOdoo() {
        // L'ajout d'une perte ne scrape plus du tout (même pas en log) : il met juste en liste.
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "type", "PERTE"))
                .when().post("/api/releve/lines")
                .then().statusCode(200)
                .body("sent", is(false))
                .body("scrapRef", nullValue());

        wiremock.verify(0, postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"create\"")));
    }
}
