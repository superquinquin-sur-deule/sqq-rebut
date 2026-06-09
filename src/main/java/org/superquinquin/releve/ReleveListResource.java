package org.superquinquin.releve;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/releves")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Relevé")
public class ReleveListResource {

    @Inject
    ReleveService service;

    @GET
    @Operation(operationId = "getReleves", summary = "Lister les relevés (récent → ancien)")
    public List<ReleveSummaryDto> list() {
        return service.listSummaries();
    }

    @GET
    @Path("/{id}")
    @Operation(operationId = "getReleveById", summary = "Lire un relevé par id")
    public ReleveDto byId(@PathParam("id") Long id) {
        return service.viewById(id);
    }
}
