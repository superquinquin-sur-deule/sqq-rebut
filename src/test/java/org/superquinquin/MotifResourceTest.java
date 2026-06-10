package org.superquinquin;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(WireMockOdooResource.class)
class MotifResourceTest {

    @Test
    void listsMotifsWithoutDlcOrigin() {
        given()
                .when().get("/api/motifs")
                .then().statusCode(200)
                .body("size()", is(3))
                .body("label", hasItem("Casse"))
                .body("id", not(hasItem(7)));   // « DLC Dépassée » exclue
    }
}
