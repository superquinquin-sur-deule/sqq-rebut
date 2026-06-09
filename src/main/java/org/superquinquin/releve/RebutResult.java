package org.superquinquin.releve;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = {"created", "dryRun", "lines"})
public record RebutResult(int created, boolean dryRun, List<ReleveLineDto> lines) {
}
