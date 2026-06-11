package org.superquinquin.product;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.superquinquin.barcode.BarcodeNomenclatureService;
import org.superquinquin.barcode.ScaleBarcodeParser;
import org.superquinquin.barcode.ScaleMatch;
import org.superquinquin.odoo.OdooClient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class ProductService {

    private static final List<String> FIELDS =
            List.of("barcode", "name", "uom_id", "list_price", "categ_id", "qty_available");

    @Inject
    OdooClient odoo;

    @Inject
    BarcodeNomenclatureService nomenclature;

    public Optional<Product> findByBarcode(String scanned) {
        return lookup(scanned)
                .or(() -> upcaToEan13(scanned).flatMap(this::lookup));
    }

    private Optional<Product> lookup(String barcode) {
        Optional<Product> exact = fetchExact(barcode);
        if (exact.isPresent()) {
            return exact;
        }
        return nomenclature.resolve(barcode)
                .flatMap(m -> fetchExact(m.baseBarcode())
                        .map(p -> p.withScannedWeight(scannedWeight(m, p))));
    }
    
    private static Optional<String> upcaToEan13(String scanned) {
        if (scanned == null || !scanned.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        if (scanned.length() == 12) {
            return Optional.of("0" + scanned);
        }
        if (scanned.length() == 11) {
            String first12 = "0" + scanned;
            return Optional.of(first12 + ScaleBarcodeParser.ean13Checksum(first12));
        }
        return Optional.empty();
    }

    private Optional<Product> fetchExact(String barcode) {
        Object domain = List.of(List.of("barcode", "=", barcode));
        JsonNode res = odoo.searchRead("product.product", domain, FIELDS, 1);
        if (res == null || !res.isArray() || res.isEmpty()) {
            return Optional.empty();
        }
        JsonNode p = res.get(0);
        String uom = m2oName(p.get("uom_id"));
        return Optional.of(new Product(
                p.path("id").asLong(),
                asText(p.get("barcode"), barcode),
                asText(p.get("name"), ""),
                rayon(p.get("categ_id")),
                uom,
                m2oId(p.get("uom_id")),
                p.path("list_price").asDouble(0),
                p.path("qty_available").asDouble(0),
                soldByWeight(uom),
                null));
    }

    private static boolean soldByWeight(String uom) {
        return uom != null && uom.toLowerCase(Locale.ROOT).startsWith("kg");
    }

    private static Double scannedWeight(ScaleMatch m, Product p) {
        return switch (m.type()) {
            case WEIGHT -> m.value();
            case PRICE_TO_WEIGHT -> p.price() > 0 ? Math.round(m.value() / p.price() * 1000) / 1000.0 : null;
        };
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
