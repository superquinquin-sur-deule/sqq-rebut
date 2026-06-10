package org.superquinquin.barcode;

import java.util.Optional;

public enum BarcodeRuleType {
    WEIGHT,
    PRICE_TO_WEIGHT;

    public static Optional<BarcodeRuleType> fromOdoo(String raw) {
        if ("weight".equals(raw)) {
            return Optional.of(WEIGHT);
        }
        if ("price_to_weight".equals(raw)) {
            return Optional.of(PRICE_TO_WEIGHT);
        }
        return Optional.empty();
    }
}
