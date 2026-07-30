package org.superquinquin.report;

import io.quarkus.qute.TemplateExtension;
import org.superquinquin.product.Uom;
import org.superquinquin.releve.ReleveLineDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Formatage des valeurs affichées dans le corps du mail (pendant back de {@code frontend/src/lib/qty.ts}).
 * Exposé à Qute comme méthodes d'extension : {@code {l.qtyLabel}}, {@code {data.dateLabel}}…
 */
@TemplateExtension
public final class ReportFormat {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReportFormat() {
    }

    /** « ×6 » pour des pièces, « 1,5 kg » pour du poids. */
    public static String qtyLabel(ReleveLineDto line) {
        if (Uom.soldByWeight(line.uom())) {
            return number(line.qty(), 3) + " kg";
        }
        return "×" + number(line.qty(), 0);
    }

    public static String dlcLabel(ReleveLineDto line) {
        return line.dlc() == null ? "—" : line.dlc().format(DAY);
    }

    public static String rayonLabel(ReleveLineDto line) {
        return line.rayon() == null || line.rayon().isBlank() ? "—" : line.rayon();
    }

    public static String dateLabel(ReportData data) {
        return data.date().format(FULL_DAY);
    }

    /** « 5 pièces / 1 kg », rappelé dans le corps du mail. */
    public static String thresholdsLabel(ReportData data) {
        return number(data.thresholdPieces(), 0) + " pièces / " + number(data.thresholdKg(), 3) + " kg";
    }

    /** Arrondi puis virgule décimale française, sans zéros inutiles : 6.0 → « 6 », 1.5 → « 1,5 ». */
    static String number(double value, int maxDecimals) {
        return BigDecimal.valueOf(value)
                .setScale(maxDecimals, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
                .replace('.', ',');
    }
}
