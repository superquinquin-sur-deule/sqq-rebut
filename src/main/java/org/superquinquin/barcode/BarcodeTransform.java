package org.superquinquin.barcode;

import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Le champ {@code transform_expr} d'une {@code barcode.rule} est une expression Python arbitraire.
 * Les balances du magasin impriment encore des prix en francs, d'où des règles en
 * {@code value / 6.55957}. On n'interprète que la forme {@code value <op> <nombre>} ; toute autre
 * forme donne {@link Unsupported}, qui fait abandonner le scan plutôt que de risquer une quantité
 * fausse (elle partirait telle quelle en {@code scrap_qty}).
 */
public sealed interface BarcodeTransform {

    Pattern EXPR = Pattern.compile("\\s*value\\s*([*/+-])\\s*(-?\\d+(?:\\.\\d+)?)\\s*");

    OptionalDouble apply(double value);

    record Identity() implements BarcodeTransform {
        @Override
        public OptionalDouble apply(double value) {
            return OptionalDouble.of(value);
        }
    }

    record Arithmetic(char op, double operand) implements BarcodeTransform {
        @Override
        public OptionalDouble apply(double value) {
            return OptionalDouble.of(switch (op) {
                case '*' -> value * operand;
                case '/' -> value / operand;
                case '+' -> value + operand;
                default -> value - operand;
            });
        }
    }

    record Unsupported(String expr) implements BarcodeTransform {
        @Override
        public OptionalDouble apply(double value) {
            return OptionalDouble.empty();
        }
    }

    static BarcodeTransform of(String expr) {
        if (expr == null || expr.isBlank()) {
            return new Identity();
        }
        Matcher m = EXPR.matcher(expr);
        if (!m.matches()) {
            return new Unsupported(expr);
        }
        char op = m.group(1).charAt(0);
        double operand = Double.parseDouble(m.group(2));
        if (op == '/' && operand == 0) {
            return new Unsupported(expr);
        }
        return new Arithmetic(op, operand);
    }
}
