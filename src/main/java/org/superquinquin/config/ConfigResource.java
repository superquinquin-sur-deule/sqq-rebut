package org.superquinquin.config;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Configuration")
public class ConfigResource {

    @ConfigProperty(name = "app.staging")
    boolean staging;

    @GET
    @Operation(operationId = "getConfig", summary = "Drapeaux d'environnement exposés au front (bannière staging, etc.)")
    public AppConfig get() {
        return new AppConfig(staging);
    }
}
