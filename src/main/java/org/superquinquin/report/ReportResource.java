package org.superquinquin.report;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/report")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Rapport")
public class ReportResource {

    @Inject
    ReportSettingsService settings;

    @Inject
    ReportScheduler scheduler;

    @Inject
    DailyReportService reports;

    @GET
    @Path("/settings")
    @Operation(operationId = "getReportSettings", summary = "Lire les réglages du rapport DLC quotidien")
    public ReportSettingsDto get() {
        return settings.get();
    }

    @PUT
    @Path("/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "updateReportSettings", summary = "Enregistrer les réglages du rapport DLC quotidien")
    public ReportSettingsDto update(ReportSettingsDto req) {
        ReportSettingsDto saved = settings.update(req);
        // Hors transaction : le job doit être replanifié sur des valeurs commitées.
        scheduler.reschedule();
        Log.infof("[RAPPORT] réglages enregistrés : actif=%s heure=%s destinataires=%d seuils=%s/%s",
                saved.enabled(), saved.sendTime(), saved.recipients().size(),
                saved.thresholdPieces(), saved.thresholdKg());
        return saved;
    }

    @POST
    @Path("/send-now")
    @Operation(operationId = "sendReportNow",
            summary = "Envoyer immédiatement le rapport DLC du jour aux destinataires enregistrés")
    public ReportSendResult sendNow() {
        Log.info("[RAPPORT] envoi manuel demandé");
        return reports.sendNow();
    }
}
