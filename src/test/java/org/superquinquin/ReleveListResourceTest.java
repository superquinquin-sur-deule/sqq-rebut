package org.superquinquin;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(WireMockOdooResource.class)
class ReleveListResourceTest {

    private void addLine(LocalDate dlc, int qty) {
        given().contentType(JSON)
                .body(Map.of("barcode", WireMockOdooResource.KNOWN_BARCODE, "dlc", dlc.toString(), "qty", qty))
                .when().post("/api/releve/lines")
                .then().statusCode(200);
    }

    @Test
    void listsAndFetchesReleveById() {
        addLine(LocalDate.now().plusDays(4), 3);

        Integer id = given().when().get("/api/releve")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("lines.size()", greaterThanOrEqualTo(1))
                .extract().path("id");

        given().when().get("/api/releves")
                .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("find { it.id == " + id + " }.lineCount", greaterThanOrEqualTo(1));

        given().when().get("/api/releves/" + id)
                .then().statusCode(200)
                .body("id", is(id))
                .body("date", is(LocalDate.now().toString()))
                .body("lines.size()", greaterThanOrEqualTo(1));
    }

    @Test
    void unknownIdReturns404() {
        given().when().get("/api/releves/999999")
                .then().statusCode(404);
    }
}
