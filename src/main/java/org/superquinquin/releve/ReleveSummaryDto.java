package org.superquinquin.releve;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

@Schema(requiredProperties = {"id", "date", "lineCount"})
public record ReleveSummaryDto(Long id, LocalDate date, long lineCount) {
}
