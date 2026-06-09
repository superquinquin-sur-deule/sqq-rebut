package org.superquinquin;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
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
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "dlc", LocalDate.now().toString(), "qty", 2))
                .when().post("/api/releve/lines")
                .then().statusCode(200);

        given().contentType(JSON).body("{}")
                .when().post("/api/releve/rebut")
                .then().statusCode(200)
                .body("dryRun", is(true))
                .body("created", greaterThanOrEqualTo(1))
                .body("lines[0].scrapRef", is("DRY-RUN"));

        // aucun create stock.scrap n'a été envoyé à Odoo
        wiremock.verify(0, postRequestedFor(urlEqualTo("/jsonrpc"))
                .withRequestBody(containing("\"create\"")));
    }
}
