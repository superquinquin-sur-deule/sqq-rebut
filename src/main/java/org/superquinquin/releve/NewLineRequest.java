package org.superquinquin.releve;

import java.time.LocalDate;

public record NewLineRequest(String barcode, LocalDate dlc, int qty) {
}
