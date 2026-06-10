package org.superquinquin.product;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(requiredProperties = {"id", "barcode", "name", "uom", "uomId", "price", "qtyAvailable", "soldByWeight"})
public record Product(
        Long id,
        String barcode,
        String name,
        String rayon,
        String uom,
        Long uomId,
        double price,
        double qtyAvailable,
        boolean soldByWeight,
        @Schema(description = "Poids (kg) extrait d'un code-barres balance, à pré-remplir") Double scannedWeight) {

    public Product withScannedWeight(Double weight) {
        return new Product(id, barcode, name, rayon, uom, uomId, price, qtyAvailable, soldByWeight, weight);
    }
}
