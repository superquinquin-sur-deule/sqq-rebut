package org.superquinquin.report;

import org.junit.jupiter.api.Test;
import org.superquinquin.releve.ReleveLineDto;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportThresholdTest {

    private static final ReportSettings SETTINGS = settings(5, 1);

    private static ReportSettings settings(double pieces, double kg) {
        ReportSettings s = new ReportSettings();
        s.thresholdPieces = pieces;
        s.thresholdKg = kg;
        return s;
    }

    private static ReleveLineDto line(String uom, double qty) {
        return new ReleveLineDto(1L, "2200145000004", "Produit", "Crèmerie", uom, "DLC",
                LocalDate.now(), "j0", null, null, qty, false, null, null);
    }

    @Test
    void piecesAreStrictlyAboveThreshold() {
        assertTrue(DailyReportService.exceedsThreshold(line("Unité(s)", 6), SETTINGS));
        assertTrue(DailyReportService.exceedsThreshold(line("Unité(s)", 5.5), SETTINGS));
        assertFalse(DailyReportService.exceedsThreshold(line("Unité(s)", 5), SETTINGS));
        assertFalse(DailyReportService.exceedsThreshold(line("Unité(s)", 1), SETTINGS));
    }

    @Test
    void weightIsComparedInKilograms() {
        assertTrue(DailyReportService.exceedsThreshold(line("kg", 1.5), SETTINGS));
        assertFalse(DailyReportService.exceedsThreshold(line("kg", 1), SETTINGS));
        assertFalse(DailyReportService.exceedsThreshold(line("kg", 0.999), SETTINGS));
        // Un produit au poids de 2 kg passerait le seuil « pièces » : c'est bien le seuil kg qui compte.
        assertFalse(DailyReportService.exceedsThreshold(line("kg", 2), settings(1, 5)));
    }

    @Test
    void uomVariantsAreClassified() {
        assertTrue(DailyReportService.exceedsThreshold(line("kg net", 1.2), SETTINGS));
        // UoM absente → traitée comme des pièces (comportement de Uom.soldByWeight).
        assertFalse(DailyReportService.exceedsThreshold(line(null, 2), SETTINGS));
        assertTrue(DailyReportService.exceedsThreshold(line(null, 6), SETTINGS));
    }

    @Test
    void thresholdsAreConfigurable() {
        ReportSettings loose = settings(0, 0);
        assertTrue(DailyReportService.exceedsThreshold(line("Unité(s)", 1), loose));
        assertTrue(DailyReportService.exceedsThreshold(line("kg", 0.1), loose));
    }
}
