package org.superquinquin.releve;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(requiredProperties = {"date", "lines"})
public record ReleveDto(Long id, LocalDate date, List<ReleveLineDto> lines) {
}
