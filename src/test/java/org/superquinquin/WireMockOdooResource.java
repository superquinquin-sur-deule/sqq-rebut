package org.superquinquin;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Stube l'endpoint JSON-RPC d'Odoo pour les tests (pas de réseau réel, pas de scrap réel).
 * Pointe {@code odoo.url} sur le serveur WireMock démarré.
 */
public class WireMockOdooResource implements QuarkusTestResourceLifecycleManager {

    public static final String KNOWN_BARCODE = "2200145000004";
    public static final String UNKNOWN_BARCODE = "0000000000000";

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

        // product.product search_read — inconnu (catch-all)
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(5)
                .withRequestBody(containing("product.product"))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":[]}")));

        // stock.scrap create → id
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("stock.scrap"))
                .withRequestBody(containing("\"create\""))
                .willReturn(okJson("{\"jsonrpc\":\"2.0\",\"result\":9999}")));

        // action_validate → true
        server.stubFor(post(urlEqualTo("/jsonrpc"))
                .atPriority(2)
                .withRequestBody(containing("action_validate"))
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
