package org.superquinquin.report;

import java.time.LocalDate;
import java.util.List;

/** Contenu du rapport du jour : groupes d'urgence non vides, et les seuils appliqués (rappelés dans le mail). */
public record ReportData(
        LocalDate date,
        List<ReportGroup> groups,
        int total,
        double thresholdPieces,
        double thresholdKg) {
}
