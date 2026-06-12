package org.superquinquin;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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
                .body("uom", is("kg"))
                .body("soldByWeight", is(true))
                .body("scannedWeight", nullValue());
    }

    @Test
    void lookupScaleBarcodeResolvesBaseProductAndWeight() {
        given()
                .when().get("/api/products/" + WireMockOdooResource.SCALE_BARCODE)
                .then().statusCode(200)
                .body("id", is(33616))
                .body("barcode", is(WireMockOdooResource.KNOWN_BARCODE))
                .body("soldByWeight", is(true))
                .body("scannedWeight", is(1.234f));
    }

    @Test
    void lookupPriceBarcodeComputesWeightFromListPrice() {
        // 7,13 € embarqués / 14,26 €/kg = 0,5 kg
        given()
                .when().get("/api/products/" + WireMockOdooResource.PRICE_SCALE_BARCODE)
                .then().statusCode(200)
                .body("id", is(33617))
                .body("barcode", is(WireMockOdooResource.PRICE_BASE_BARCODE))
                .body("soldByWeight", is(true))
                .body("scannedWeight", is(0.5f));
    }

    @Test
    void lookupUpcaScanWithoutSystemCharIsNormalized() {
        given()
                .when().get("/api/products/" + WireMockOdooResource.PRICE_SCALE_BARCODE.substring(1))
                .then().statusCode(200)
                .body("id", is(33617))
                .body("barcode", is(WireMockOdooResource.PRICE_BASE_BARCODE))
                .body("scannedWeight", is(0.5f));
    }

    @Test
    void lookupUpcaScanWithoutSystemCharNorCheckDigitIsNormalized() {
        given()
                .when().get("/api/products/" + WireMockOdooResource.PRICE_SCALE_BARCODE.substring(1, 12))
                .then().statusCode(200)
                .body("id", is(33617))
                .body("barcode", is(WireMockOdooResource.PRICE_BASE_BARCODE))
                .body("scannedWeight", is(0.5f));
    }

    @Test
    void lookupUnknownBarcodeReturns404() {
        given()
                .when().get("/api/products/" + WireMockOdooResource.UNKNOWN_BARCODE)
                .then().statusCode(404);
    }

    @Test
    void searchKnownQueryMapsList() {
        given()
                .when().get("/api/products?q=" + WireMockOdooResource.KNOWN_QUERY)
                .then().statusCode(200)
                .body("size()", is(2))
                .body("[0].id", is(33616))
                .body("[0].rayon", is("Viandes locales"))
                .body("[0].soldByWeight", is(true))
                .body("[1].name", is("Saucisse de Toulouse x4"))
                .body("[1].soldByWeight", is(false));
    }

    @Test
    void searchUnknownQueryReturnsEmptyList() {
        given()
                .when().get("/api/products?q=zzzznope")
                .then().statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void searchShortQueryReturnsEmptyListWithoutOdooCall() {
        given()
                .when().get("/api/products?q=ab")
                .then().statusCode(200)
                .body("size()", is(0));
    }
}
