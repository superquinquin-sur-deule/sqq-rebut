package org.superquinquin.config;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Configuration runtime exposée au front (drapeaux d'environnement). */
@Schema(requiredProperties = {"staging"})
public record AppConfig(boolean staging) {
}
