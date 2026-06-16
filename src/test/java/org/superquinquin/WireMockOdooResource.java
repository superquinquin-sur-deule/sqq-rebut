package org.superquinquin;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class WireMockOdooResource implements QuarkusTestResourceLifecycleManager {

    public static final String KNOWN_BARCODE = "2200145000004";
    public static final String UNKNOWN_BARCODE = "0000000000000";
    public static final String SCALE_BARCODE = "2200145012342";
    public static final String PRICE_SCALE_BARCODE = "0200145007135";
    public static final String PRICE_BASE_BARCODE = "0200145000006";
    public static final String KNOWN_QUERY = "saucisse";
    public static final long NO_BARCODE_PRODUCT_ID = 51000;
    public static final String NO_BARCODE_QUERY = "salade";
    // Origine « Pertes Fruits et légumes » : utilisée comme déclencheur du scénario « stock
    // insuffisant » dans les stubs (le stock des F&L n'est pas saisi dans Odoo).
    public static final int INSUFFICIENT_STOCK_ORIGIN_ID = 11;
    public static final int INSUFFICIENT_STOCK_SCRAP_ID = 8888;

    private WireMockServer server;

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(options().dynamicPort());
        server.start();

        // common.login → uid
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(1)
                .withRequestBody(containing("\"login\""))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":565}")));

        // product.product search_read — produit connu
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("product.product"))
                .withRequestBody(containing(KNOWN_BARCODE))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":[{"
                        + "\"id\":33616,"
                        + "\"barcode\":\"" + KNOWN_BARCODE + "\","
                        + "\"name\":\"FERME DU CRUSOBEAU Saucisse fine fromage\","
                        + "\"uom_id\":[3,\"kg\"],"
                        + "\"list_price\":14.26,"
                        + "\"categ_id\":[239,\"Produits frais / Viandes locales\"],"
                        + "\"qty_available\":2.248}]}")));

        // product.product search_read — produit au poids dont la base est un code prix embarqué
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("product.product"))
                .withRequestBody(containing(PRICE_BASE_BARCODE))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":[{"
                        + "\"id\":33617,"
                        + "\"barcode\":\"" + PRICE_BASE_BARCODE + "\","
                        + "\"name\":\"FERME DU CRUSOBEAU Rôti de porc\","
                        + "\"uom_id\":[3,\"kg\"],"
                        + "\"list_price\":14.26,"
                        + "\"categ_id\":[239,\"Produits frais / Viandes locales\"],"
                        + "\"qty_available\":1.5}]}")));

        // product.product search_read par nom (ilike) — requête connue
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("ilike"))
                .withRequestBody(containing(KNOWN_QUERY))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":[{"
                        + "\"id\":33616,"
                        + "\"barcode\":\"" + KNOWN_BARCODE + "\","
                        + "\"name\":\"FERME DU CRUSOBEAU Saucisse fine fromage\","
                        + "\"uom_id\":[3,\"kg\"],"
                        + "\"list_price\":14.26,"
                        + "\"categ_id\":[239,\"Produits frais / Viandes locales\"],"
                        + "\"qty_available\":2.248},{"
                        + "\"id\":40001,"
                        + "\"barcode\":\"3270190001234\","
                        + "\"name\":\"Saucisse de Toulouse x4\","
                        + "\"uom_id\":[1,\"Unité(s)\"],"
                        + "\"list_price\":4.5,"
                        + "\"categ_id\":[240,\"Produits frais / Charcuterie\"],"
                        + "\"qty_available\":6.0}]}")));

        // product.product search_read par id — produit sans code-barres (salade)
        // NB : matcher le fragment ["id","=", et non "id" seul (product_id/uom_id/categ_id contiennent « id »).
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("product.product"))
                .withRequestBody(containing("[\"id\",\"=\","))
                .withRequestBody(containing(String.valueOf(NO_BARCODE_PRODUCT_ID)))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":[{"
                        + "\"id\":" + NO_BARCODE_PRODUCT_ID + ","
                        + "\"barcode\":false,"
                        + "\"name\":\"Salade verte\","
                        + "\"uom_id\":[1,\"Unité(s)\"],"
                        + "\"list_price\":1.2,"
                        + "\"categ_id\":[300,\"Produits frais / Fruits et légumes\"],"
                        + "\"qty_available\":12.0}]}")));

        // product.product search_read par nom (ilike) — produit sans code-barres
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("ilike"))
                .withRequestBody(containing(NO_BARCODE_QUERY))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":[{"
                        + "\"id\":" + NO_BARCODE_PRODUCT_ID + ","
                        + "\"barcode\":false,"
                        + "\"name\":\"Salade verte\","
                        + "\"uom_id\":[1,\"Unité(s)\"],"
                        + "\"list_price\":1.2,"
                        + "\"categ_id\":[300,\"Produits frais / Fruits et légumes\"],"
                        + "\"qty_available\":12.0}]}")));

        // barcode.rule search_read — règles balance (la règle encoding!=ean13 doit être ignorée)
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("barcode.rule"))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":["
                        + "{\"id\":119,\"name\":\"Weight Barcodes 3 Decimals (22)\",\"sequence\":22,"
                        + "\"encoding\":\"ean13\",\"type\":\"weight\",\"pattern\":\"22.....{NNDDD}\"},"
                        + "{\"id\":118,\"name\":\"Price Look Up Codes (PLU Codes)\",\"sequence\":16,"
                        + "\"encoding\":\"ean13\",\"type\":\"price_to_weight\",\"pattern\":\"02.....{NNNDD}\"},"
                        + "{\"id\":7,\"name\":\"Any-encoding rule\",\"sequence\":1,"
                        + "\"encoding\":\"any\",\"type\":\"weight\",\"pattern\":\"21.....{NNDDD}\"}]}")));

        // product.product search_read — inconnu (catch-all)
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(5)
                .withRequestBody(containing("product.product"))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":[]}")));

        // stock.scrap.origin search_read — motifs de rupture (y compris « DLC Dépassée », id 7)
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("stock.scrap.origin"))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":["
                        + "{\"id\":7,\"name\":\"DLC Dépassée\"},"
                        + "{\"id\":8,\"name\":\"Casse\"},"
                        + "{\"id\":9,\"name\":\"Consommable\"},"
                        + "{\"id\":11,\"name\":\"Pertes Fruits et légumes\"}]}")));

        // stock.scrap create — origine 11 (« Pertes Fruits et légumes ») → id distinct, sert à
        // simuler le cas « stock insuffisant » (le stock des F&L n'est pas saisi dans Odoo).
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(1)
                .withRequestBody(containing("stock.scrap"))
                .withRequestBody(containing("\"create\""))
                .withRequestBody(containing("\"scrap_origin_id\":" + INSUFFICIENT_STOCK_ORIGIN_ID))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":" + INSUFFICIENT_STOCK_SCRAP_ID + "}")));

        // stock.scrap create → id
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("stock.scrap"))
                .withRequestBody(containing("\"create\""))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":9999}")));

        // action_validate sur le scrap « stock insuffisant » → wizard quantité insuffisante (un
        // objet, pas `true`) : Odoo laisse le scrap en brouillon tant qu'on ne confirme pas.
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(1)
                .withRequestBody(containing("action_validate"))
                .withRequestBody(containing(String.valueOf(INSUFFICIENT_STOCK_SCRAP_ID)))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":{"
                        + "\"type\":\"ir.actions.act_window\","
                        + "\"res_model\":\"stock.warn.insufficient.qty.scrap\","
                        + "\"target\":\"new\"}}")));

        // action_validate → true (stock suffisant : Odoo valide directement)
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("action_validate"))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":true}")));

        // do_scrap → true (forçage de la validation après le wizard)
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("do_scrap"))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":true}")));

        return Map.of("odoo.url", server.baseUrl());
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(server,
                new TestInjector.AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class));
    }
}
