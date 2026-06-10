package org.superquinquin.barcode;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaleBarcodeParserTest {

    private static CompiledRule rule(String pattern, BarcodeRuleType type, int sequence) {
        return ScaleBarcodeParser.compile("test " + pattern, sequence, type, pattern).orElseThrow();
    }

    @Test
    void compileSplitsIntAndDecimalDigits() {
        CompiledRule r = rule("22.....{NNDDD}", BarcodeRuleType.WEIGHT, 1);
        assertEquals(2, r.intDigits());
        assertEquals(3, r.decDigits());

        CompiledRule price = rule("02.....{NNNDD}", BarcodeRuleType.PRICE_TO_WEIGHT, 1);
        assertEquals(3, price.intDigits());
        assertEquals(2, price.decDigits());
    }

    @Test
    void compileRejectsPatternsWithoutValueBlock() {
        assertTrue(ScaleBarcodeParser.compile("x", 1, BarcodeRuleType.WEIGHT, ".*").isEmpty());
        assertTrue(ScaleBarcodeParser.compile("x", 1, BarcodeRuleType.WEIGHT, "041").isEmpty());
        assertTrue(ScaleBarcodeParser.compile("x", 1, BarcodeRuleType.WEIGHT, null).isEmpty());
        assertTrue(ScaleBarcodeParser.compile("x", 1, BarcodeRuleType.WEIGHT, "22{}..").isEmpty());
    }

    @Test
    void matchExtractsWeightAndRebuildsBaseBarcode() {
        // 2200145012342 = produit 2200145, poids 01.234 kg, clé recalculée
        Optional<ScaleMatch> m = ScaleBarcodeParser.match(
                List.of(rule("22.....{NNDDD}", BarcodeRuleType.WEIGHT, 1)), "2200145012342");
        assertEquals(1.234, m.orElseThrow().value(), 1e-9);
        assertEquals("2200145000004", m.orElseThrow().baseBarcode());
        assertEquals(BarcodeRuleType.WEIGHT, m.orElseThrow().type());
    }

    @Test
    void matchExtractsEmbeddedPrice() {
        Optional<ScaleMatch> m = ScaleBarcodeParser.match(
                List.of(rule("02.....{NNNDD}", BarcodeRuleType.PRICE_TO_WEIGHT, 1)), "0200145007135");
        assertEquals(7.13, m.orElseThrow().value(), 1e-9);
        assertEquals("0200145000006", m.orElseThrow().baseBarcode());
    }

    @Test
    void matchHonoursLiteralDigitsInPattern() {
        List<CompiledRule> rules = List.of(rule("0244...{NNDDD}", BarcodeRuleType.WEIGHT, 1));
        assertEquals(1.5, ScaleBarcodeParser.match(rules, "0244123015003").orElseThrow().value(), 1e-9);
        assertTrue(ScaleBarcodeParser.match(rules, "0245123015003").isEmpty());
    }

    @Test
    void firstRuleInListWins() {
        // la liste est triée par sequence en amont : la plus spécifique d'abord
        List<CompiledRule> rules = List.of(
                rule("2200145{NNDDD}", BarcodeRuleType.PRICE_TO_WEIGHT, 5),
                rule("22.....{NNDDD}", BarcodeRuleType.WEIGHT, 10));
        assertEquals(BarcodeRuleType.PRICE_TO_WEIGHT,
                ScaleBarcodeParser.match(rules, "2200145012342").orElseThrow().type());
    }

    @Test
    void matchRejectsNonEan13Scans() {
        List<CompiledRule> rules = List.of(rule("22.....{NNDDD}", BarcodeRuleType.WEIGHT, 1));
        assertTrue(ScaleBarcodeParser.match(rules, "220014501234").isEmpty());   // 12 digits
        assertTrue(ScaleBarcodeParser.match(rules, "22001450123421").isEmpty()); // 14 digits
        assertTrue(ScaleBarcodeParser.match(rules, "22001450A2342").isEmpty());  // lettre
        assertTrue(ScaleBarcodeParser.match(rules, null).isEmpty());
        assertTrue(ScaleBarcodeParser.match(rules, "9900145012342").isEmpty());  // aucun pattern
    }

    @Test
    void ean13Checksum() {
        assertEquals(4, ScaleBarcodeParser.ean13Checksum("220014500000"));
        assertEquals(2, ScaleBarcodeParser.ean13Checksum("220014501234"));
        assertEquals(0, ScaleBarcodeParser.ean13Checksum("000000000000"));
    }
}
