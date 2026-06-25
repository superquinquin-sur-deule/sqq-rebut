package org.superquinquin.releve;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

@Schema(requiredProperties = {"id", "name", "type", "qty", "sent"})
public record ReleveLineDto(
        Long id,
        String barcode,
        String name,
        String rayon,
        String uom,
        String type,
        LocalDate dlc,
        String urgency,
        Long motifId,
        String motifLabel,
        double qty,
        boolean sent,
        String scrapRef,
        Double qtyAvailable) {

    public static ReleveLineDto from(ReleveLine l, LocalDate today) {
        String urgency = l.type == LineType.DLC && l.dlc != null ? Urgency.of(l.dlc, today).key() : null;
        return new ReleveLineDto(l.id, l.barcode, l.name, l.rayon, l.uom, l.type.name(), l.dlc, urgency,
                l.motifId, l.motifLabel, l.qty, l.sent, l.scrapRef, l.qtyAvailable);
    }
}
