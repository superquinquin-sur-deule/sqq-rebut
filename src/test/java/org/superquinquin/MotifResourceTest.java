package org.superquinquin;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(WireMockOdooResource.class)
class MotifResourceTest {

    @Test
    void listsMotifsIncludingDlcOrigin() {
        given()
                .when().get("/api/motifs")
                .then().statusCode(200)
                .body("size()", is(4))
                .body("label", hasItem("Casse"))
                .body("id", hasItem(7));
    }
}
