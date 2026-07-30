package org.superquinquin.report;

import jakarta.enterprise.context.ApplicationScoped;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.superquinquin.releve.ReleveLineDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Génère le classeur joint au rapport quotidien (une feuille, une ligne par produit). */
@ApplicationScoped
public class XlsxReportWriter {

    private static final String[] HEADERS =
            {"Rayon", "Produit", "Code-barres", "DLC", "Urgence", "Quantité", "Unité"};

    private static final String HEADER_COLOR = "F1DC43";
    private static final int PRODUCT_COLUMN = 1;

    public byte[] write(ReportData data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Workbook wb = new Workbook(out, "Rebut SuperQuinquin", "1.0");
            Worksheet ws = wb.newWorksheet("DLC " + data.date());
            for (int c = 0; c < HEADERS.length; c++) {
                ws.value(0, c, HEADERS[c]);
                ws.width(c, c == PRODUCT_COLUMN ? 44 : 16);
            }
            ws.range(0, 0, 0, HEADERS.length - 1).style().bold().fillColor(HEADER_COLOR).set();
            ws.freezePane(0, 1);

            int r = 1;
            for (ReportGroup g : data.groups()) {
                for (ReleveLineDto l : g.lines()) {
                    text(ws, r, 0, l.rayon());
                    text(ws, r, 1, l.name());
                    text(ws, r, 2, l.barcode());
                    text(ws, r, 3, l.dlc() != null ? l.dlc().toString() : null);
                    text(ws, r, 4, g.tag());
                    ws.value(r, 5, l.qty());
                    text(ws, r, 6, l.uom());
                    r++;
                }
            }
            // finish() écrit le répertoire central du ZIP : sans lui, le fichier est illisible.
            wb.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Génération du classeur XLSX échouée", e);
        }
    }

    /** Évite l'ambiguïté de {@code value(int, int, null)} entre les surcharges String/Number. */
    private static void text(Worksheet ws, int row, int col, String value) {
        if (value != null) {
            ws.value(row, col, value);
        }
    }
}
