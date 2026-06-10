package org.superquinquin.motif;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/motifs")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Motifs")
public class MotifResource {

    @Inject
    MotifService service;

    @GET
    @Operation(operationId = "getMotifs", summary = "Lister les motifs de rupture (Odoo stock.scrap.origin, hors « DLC Dépassée »)")
    public List<Motif> list() {
        return service.list();
    }
}
