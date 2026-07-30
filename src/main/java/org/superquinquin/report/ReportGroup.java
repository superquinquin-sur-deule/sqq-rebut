package org.superquinquin.report;

import org.superquinquin.releve.ReleveLineDto;

import java.util.List;

/** Un bloc d'urgence du rapport (J-0, J-1 ou J-2) et ses lignes au-delà des seuils. */
public record ReportGroup(String key, String tag, String label, List<ReleveLineDto> lines) {
}
