package org.superquinquin.releve;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum Urgency {
    J0("j0"),
    J1("j1"),
    J2("j2");

    private final String key;

    Urgency(String key) {
        this.key = key;
    }

    /** Clé minuscule attendue par le front (classes CSS j0/j1/j2). */
    public String key() {
        return key;
    }

    public static Urgency of(LocalDate dlc, LocalDate today) {
        long diff = ChronoUnit.DAYS.between(today, dlc);
        if (diff <= 0) {
            return J0;
        }
        if (diff == 1) {
            return J1;
        }
        return J2;
    }
}
