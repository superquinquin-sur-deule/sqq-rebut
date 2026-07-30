package org.superquinquin.report;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.superquinquin.brevo.BrevoClient;
import org.superquinquin.brevo.BrevoMail;
import org.superquinquin.product.Uom;
import org.superquinquin.releve.LineType;
import org.superquinquin.releve.ReleveDto;
import org.superquinquin.releve.ReleveLineDto;
import org.superquinquin.releve.ReleveService;
import org.superquinquin.releve.Urgency;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Construit et envoie le rapport DLC quotidien : lignes DLC du relevé du jour dont la quantité
 * dépasse (strictement) les seuils réglés, groupées par urgence J-0 / J-1 / J-2.
 */
@ApplicationScoped
public class DailyReportService {

    private static final DateTimeFormatter SUBJECT_DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject
    ReleveService releves;

    @Inject
    ReportSettingsService settings;

    @Inject
    XlsxReportWriter xlsx;

    @Inject
    BrevoClient brevo;

    /** Déclenché par le planificateur : consomme la journée, ne laisse jamais fuiter d'exception. */
    public void runScheduled() {
        LocalDate today = settings.today();
        if (!settings.claimToday(today)) {
            Log.infof("[RAPPORT] déjà envoyé le %s, envoi ignoré", today);
            return;
        }
        try {
            doSend(today, settings.current());
            settings.recordResult(ReportSettingsService.STATUS_OK, null);
        } catch (RuntimeException e) {
            // La journée reste consommée : le bouton « Envoyer maintenant » sert de rattrapage.
            Log.error("[RAPPORT] envoi du rapport quotidien échoué", e);
            settings.recordResult(ReportSettingsService.STATUS_ERROR, String.valueOf(e.getMessage()));
        }
    }

    /**
     * Envoi manuel. Ne consomme pas la journée (tester le matin ne doit pas annuler l'envoi du soir)
     * et laisse remonter l'erreur au client HTTP.
     */
    public ReportSendResult sendNow() {
        LocalDate today = settings.today();
        try {
            ReportSendResult result = doSend(today, settings.current());
            settings.recordResult(ReportSettingsService.STATUS_OK, null);
            return result;
        } catch (RuntimeException e) {
            settings.recordResult(ReportSettingsService.STATUS_ERROR, String.valueOf(e.getMessage()));
            throw e;
        }
    }

    /**
     * Rattrapage au démarrage : un redéploiement pile à l'heure d'envoi ferait sinon sauter le rapport,
     * le planificateur ne rejouant pas les déclenchements manqués.
     */
    public void catchUpIfMissed() {
        ReportSettings s = settings.current();
        LocalDate today = settings.today();
        if (s.enabled && !today.equals(s.lastSentDate)
                && !LocalTime.now(settings.zone()).isBefore(s.sendTime)) {
            Log.info("[RAPPORT] rapport du jour manqué (redémarrage ?) : rattrapage");
            runScheduled();
        }
    }

    private ReportSendResult doSend(LocalDate date, ReportSettings s) {
        List<String> to = ReportSettingsService.recipientsOf(s);
        ReportData data = build(date, s);
        byte[] file = data.total() > 0 ? xlsx.write(data) : null;
        String name = "rapport-dlc-" + date + ".xlsx";
        String html = ReportTemplates.dailyDlc(data).render();
        String messageId = brevo.send(new BrevoMail(subject(data), html, to, name, file));
        return new ReportSendResult(data.total(), to, messageId);
    }

    /** Lignes DLC du relevé du jour au-delà des seuils, groupées par urgence (J-0, puis J-1, puis J-2). */
    public ReportData build(LocalDate date, ReportSettings s) {
        ReleveDto releve = releves.view(date);
        List<ReportGroup> groups = new ArrayList<>();
        int total = 0;
        for (Urgency u : Urgency.values()) {
            List<ReleveLineDto> lines = releve.lines().stream()
                    .filter(l -> LineType.DLC.name().equals(l.type()))
                    .filter(l -> u.key().equals(l.urgency()))
                    .filter(l -> exceedsThreshold(l, s))
                    .sorted(Comparator
                            .comparing((ReleveLineDto l) -> l.rayon() == null ? "" : l.rayon())
                            .thenComparing(ReleveLineDto::name))
                    .toList();
            if (!lines.isEmpty()) {
                groups.add(new ReportGroup(u.key(), u.tag(), u.label(), lines));
                total += lines.size();
            }
        }
        return new ReportData(date, List.copyOf(groups), total, s.thresholdPieces, s.thresholdKg);
    }

    /** Strictement supérieur au seuil : plus d'1 kg pour le poids, plus de 5 pièces sinon (valeurs réglables). */
    static boolean exceedsThreshold(ReleveLineDto l, ReportSettings s) {
        return Uom.soldByWeight(l.uom()) ? l.qty() > s.thresholdKg : l.qty() > s.thresholdPieces;
    }

    static String subject(ReportData data) {
        String day = data.date().format(SUBJECT_DAY);
        if (data.total() == 0) {
            return "Rapport DLC du " + day + " — rien à signaler";
        }
        return "Rapport DLC du " + day + " — " + data.total()
                + (data.total() > 1 ? " produits" : " produit");
    }
}
