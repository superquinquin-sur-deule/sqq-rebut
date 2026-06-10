package org.superquinquin.barcode;

import java.util.regex.Pattern;

public record CompiledRule(
        String name,
        int sequence,
        BarcodeRuleType type,
        Pattern regex,
        int intDigits,
        int decDigits) {
}
