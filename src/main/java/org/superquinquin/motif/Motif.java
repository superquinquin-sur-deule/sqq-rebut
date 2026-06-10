package org.superquinquin.motif;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(requiredProperties = {"id", "label"})
public record Motif(long id, String label) {
}
