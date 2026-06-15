package org.superquinquin.releve;

import java.time.LocalDate;

public record NewLineRequest(String barcode, Long productId, LocalDate dlc, double qty, String type, Long motifId) {
}
