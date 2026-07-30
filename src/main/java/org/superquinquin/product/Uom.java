package org.superquinquin.product;

import java.util.Locale;

/** Unité de mesure Odoo : discrimine les produits vendus au poids (« kg », « kg net »…). */
public final class Uom {

    private Uom() {
    }

    public static boolean soldByWeight(String uom) {
        return uom != null && uom.toLowerCase(Locale.ROOT).startsWith("kg");
    }
}
