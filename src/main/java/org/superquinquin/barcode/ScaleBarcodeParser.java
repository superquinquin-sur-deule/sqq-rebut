package org.superquinquin.barcode;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScaleBarcodeParser {

    private ScaleBarcodeParser() {
    }

    public static Optional<CompiledRule> compile(String name, int sequence, BarcodeRuleType type, String pattern) {
        if (pattern == null) {
            return Optional.empty();
        }
        int open = pattern.indexOf('{');
        int close = pattern.indexOf('}');
        if (open < 0 || close < open || pattern.indexOf('{', close) >= 0) {
            return Optional.empty();
        }
        String block = pattern.substring(open + 1, close);
        int intDigits = count(block, 'N');
        int decDigits = count(block, 'D');
        if (intDigits + decDigits == 0 || intDigits + decDigits != block.length()) {
            return Optional.empty();
        }
        StringBuilder regex = new StringBuilder();
        appendLiteral(regex, pattern.substring(0, open));
        regex.append("(\\d{").append(intDigits + decDigits).append("})");
        appendLiteral(regex, pattern.substring(close + 1));
        return Optional.of(new CompiledRule(name, sequence, type,
                Pattern.compile(regex.toString()), intDigits, decDigits));
    }

    /** Première règle qui matche en préfixe d'un scan EAN-13 (la liste doit être triée par sequence). */
    public static Optional<ScaleMatch> match(List<CompiledRule> rules, String scanned) {
        if (scanned == null || scanned.length() != 13 || !scanned.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        for (CompiledRule rule : rules) {
            Matcher m = rule.regex().matcher(scanned);
            if (!m.lookingAt()) {
                continue;
            }
            double value = Integer.parseInt(m.group(1)) / Math.pow(10, rule.decDigits());
            String zeroed = scanned.substring(0, m.start(1))
                    + "0".repeat(m.end(1) - m.start(1))
                    + scanned.substring(m.end(1));
            String base12 = zeroed.substring(0, 12);
            return Optional.of(new ScaleMatch(base12 + ean13Checksum(base12), rule.type(), value));
        }
        return Optional.empty();
    }

    public static int ean13Checksum(String first12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = first12.charAt(i) - '0';
            sum += i % 2 == 0 ? digit : digit * 3;
        }
        return (10 - sum % 10) % 10;
    }

    private static void appendLiteral(StringBuilder regex, String segment) {
        for (char c : segment.toCharArray()) {
            if (c == '.') {
                regex.append("\\d");
            } else if (Character.isDigit(c)) {
                regex.append(c);
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
    }

    private static int count(String s, char c) {
        return (int) s.chars().filter(x -> x == c).count();
    }
}
