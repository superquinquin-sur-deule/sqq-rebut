package org.superquinquin.releve;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

@Schema(requiredProperties = {"id", "barcode", "name", "dlc", "urgency", "qty", "sent"})
public record ReleveLineDto(
        Long id,
        String barcode,
        String name,
        String rayon,
        String uom,
        LocalDate dlc,
        String urgency,
        int qty,
        boolean sent,
        String scrapRef) {

    public static ReleveLineDto from(ReleveLine l, LocalDate today) {
        return new ReleveLineDto(l.id, l.barcode, l.name, l.rayon, l.uom, l.dlc,
                Urgency.of(l.dlc, today).key(), l.qty, l.sent, l.scrapRef);
    }
}
