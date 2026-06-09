package org.superquinquin.product;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(requiredProperties = {"id", "barcode", "name", "uom", "uomId", "price", "qtyAvailable"})
public record Product(
        Long id,
        String barcode,
        String name,
        String rayon,
        String uom,
        Long uomId,
        double price,
        double qtyAvailable) {
}
