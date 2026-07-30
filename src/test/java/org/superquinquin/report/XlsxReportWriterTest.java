package org.superquinquin.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.superquinquin.releve.ReleveLineDto;
import org.superquinquin.releve.Urgency;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XlsxReportWriterTest {

    private static final LocalDate DAY = LocalDate.of(2026, 7, 29);

    private static ReportData data() {
        ReleveLineDto weight = new ReleveLineDto(1L, "2200145000004", "Saucisse fine", "Viandes locales",
                "kg", "DLC", DAY, "j0", null, null, 2.5, false, null, null);
        ReleveLineDto pieces = new ReleveLineDto(2L, null, "Salade verte", "Fruits et légumes",
                "Unité(s)", "DLC", DAY.plusDays(1), "j1", null, null, 8, false, null, null);
        return new ReportData(DAY, List.of(
                new ReportGroup(Urgency.J0.key(), Urgency.J0.tag(), Urgency.J0.label(), List.of(weight)),
                new ReportGroup(Urgency.J1.key(), Urgency.J1.tag(), Urgency.J1.label(), List.of(pieces))),
                2, 5, 1);
    }

    @Test
    void writesAReadableWorkbook(@TempDir Path tmp) throws Exception {
        byte[] xlsx = new XlsxReportWriter().write(data());

        assertTrue(xlsx.length > 1000, "classeur trop petit : " + xlsx.length + " octets");
        assertTrue(xlsx[0] == 'P' && xlsx[1] == 'K', "signature ZIP absente");

        // Lecture par le répertoire central, comme Excel : un classeur non finalisé serait rejeté ici.
        Path file = tmp.resolve("rapport.xlsx");
        Files.write(file, xlsx);
        try (ZipFile zip = new ZipFile(file.toFile())) {
            assertNotNull(zip.getEntry("xl/workbook.xml"), "classeur absent de l'archive");
            assertNotNull(zip.getEntry("xl/worksheets/sheet1.xml"), "feuille absente de l'archive");
            // fastexcel peut ranger le texte en table de chaînes partagées : on cherche dans les deux.
            String content = entryText(zip, "xl/worksheets/sheet1.xml")
                    + entryText(zip, "xl/sharedStrings.xml");
            assertTrue(content.contains("Saucisse fine"), "produit absent du classeur");
            assertTrue(content.contains("Rayon"), "en-têtes absents du classeur");
        }
    }

    private static String entryText(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            return "";
        }
        try (var in = zip.getInputStream(entry)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void toleratesMissingBarcodeAndRayon() {
        ReleveLineDto bare = new ReleveLineDto(3L, null, "Produit nu", null, null, "DLC",
                null, "j0", null, null, 9, false, null, null);
        ReportData data = new ReportData(DAY, List.of(
                new ReportGroup("j0", "J-0", "Périme aujourd'hui", List.of(bare))), 1, 5, 1);

        assertNotNull(new XlsxReportWriter().write(data));
    }
}
