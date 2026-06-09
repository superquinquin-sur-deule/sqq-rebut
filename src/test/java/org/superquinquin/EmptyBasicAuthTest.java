package org.superquinquin;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

@QuarkusTest
@QuarkusTestResource(WireMockOdooResource.class)
@TestProfile(EmptyBasicAuthTest.EmptyBasicAuthProfile.class)
class EmptyBasicAuthTest {

    public static class EmptyBasicAuthProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "odoo.basic-auth.user", "",
                    "odoo.basic-auth.password", "");
        }
    }

    @Test
    void bootsAndLooksUpWithoutBasicAuthHeader() {
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE,
                        "dlc", LocalDate.now().toString(), "qty", 1))
                .when().post("/api/releve/lines")
                .then().statusCode(200);
    }
}
