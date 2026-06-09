package org.superquinquin.product;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.superquinquin.odoo.OdooClient;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductService {

    private static final List<String> FIELDS =
            List.of("barcode", "name", "uom_id", "list_price", "categ_id", "qty_available");

    @Inject
    OdooClient odoo;

    public Optional<Product> findByBarcode(String barcode) {
        Object domain = List.of(List.of("barcode", "=", barcode));
        JsonNode res = odoo.searchRead("product.product", domain, FIELDS, 1);
        if (res == null || !res.isArray() || res.isEmpty()) {
            return Optional.empty();
        }
        JsonNode p = res.get(0);
        return Optional.of(new Product(
                p.path("id").asLong(),
                asText(p.get("barcode"), barcode),
                asText(p.get("name"), ""),
                rayon(p.get("categ_id")),
                m2oName(p.get("uom_id")),
                m2oId(p.get("uom_id")),
                p.path("list_price").asDouble(0),
                p.path("qty_available").asDouble(0)));
    }

    private static String asText(JsonNode n, String dflt) {
        return n == null || n.isNull() || !n.isTextual() ? dflt : n.asText();
    }

    private static String m2oName(JsonNode n) {
        return n != null && n.isArray() && n.size() == 2 ? n.get(1).asText() : null;
    }

    private static Long m2oId(JsonNode n) {
        return n != null && n.isArray() && n.size() == 2 ? n.get(0).asLong() : null;
    }

    private static String rayon(JsonNode n) {
        String full = m2oName(n);
        if (full == null) {
            return null;
        }
        int i = full.lastIndexOf('/');
        return i >= 0 ? full.substring(i + 1).strip() : full.strip();
    }
}
