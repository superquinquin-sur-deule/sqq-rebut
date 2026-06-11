package org.superquinquin.releve;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;

@Path("/api/releve")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Relevé")
public class ReleveResource {

    @Inject
    ReleveService service;

    @GET
    @Operation(operationId = "getReleve", summary = "Lire le relevé d'une date (défaut : aujourd'hui)")
    public ReleveDto get(@QueryParam("date") String date) {
        LocalDate d = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        return service.view(d);
    }

    @POST
    @Path("/lines")
    @Operation(operationId = "addLine", summary = "Ajouter une ligne scannée")
    public ReleveLineDto add(NewLineRequest req) {
        Log.infof("Ajout d'une ligne scannée: barcode=%s, qty=%s, motifId=%s, type=%s", req.barcode(), req.qty(), req.motifId(), req.type());
        return service.addLine(req);
    }

    @PUT
    @Path("/lines/{id}")
    @Operation(operationId = "updateLine", summary = "Modifier la quantité d'une ligne")
    public ReleveLineDto update(@PathParam("id") Long id, UpdateLineRequest req) {
        Log.infof("Modification d'une ligne: id=%s, qty=%s", id, req.qty());
        return service.updateLine(id, req.qty());
    }

    @DELETE
    @Path("/lines/{id}")
    @Operation(operationId = "deleteLine", summary = "Supprimer une ligne")
    public void delete(@PathParam("id") Long id) {
        Log.infof("Suppression d'une ligne: id=%s", id);
        service.delete(id);
    }

    @POST
    @Path("/rebut")
    @Operation(operationId = "sendRebut",
            summary = "Envoyer les DLC J-0 au rebut (stock.scrap Odoo)")
    public RebutResult rebut(RebutRequest req) {
        Log.infof("Envoi de rebut: lineIds=%s", req != null ? req.lineIds() : "tous");
        return service.sendRebut(req != null ? req.lineIds() : null);
    }
}
