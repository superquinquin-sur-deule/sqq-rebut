package org.superquinquin;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(WireMockOdooResource.class)
class ProductResourceTest {

    @Test
    void lookupKnownBarcodeMapsDto() {
        given()
                .when().get("/api/products/" + WireMockOdooResource.KNOWN_BARCODE)
                .then().statusCode(200)
                .body("id", is(33616))
                .body("name", is("FERME DU CRUSOBEAU Saucisse fine fromage"))
                .body("rayon", is("Viandes locales"))   // feuille de categ_id
                .body("uom", is("kg"));
    }

    @Test
    void lookupUnknownBarcodeReturns404() {
        given()
                .when().get("/api/products/" + WireMockOdooResource.UNKNOWN_BARCODE)
                .then().statusCode(404);
    }
}
