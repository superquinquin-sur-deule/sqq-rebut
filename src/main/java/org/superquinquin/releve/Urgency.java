package org.superquinquin.releve;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum Urgency {
    J0("j0", "J-0", "Périme aujourd'hui"),
    J1("j1", "J-1", "Périme demain"),
    J2("j2", "J-2", "Périme dans 2 jours");

    private final String key;
    private final String tag;
    private final String label;

    Urgency(String key, String tag, String label) {
        this.key = key;
        this.tag = tag;
        this.label = label;
    }

    /** Clé minuscule attendue par le front (classes CSS j0/j1/j2). */
    public String key() {
        return key;
    }

    /** Étiquette courte affichée dans le rapport (pendant back du URG côté front). */
    public String tag() {
        return tag;
    }

    public String label() {
        return label;
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
