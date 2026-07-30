package org.superquinquin.report;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.time.LocalTime;

/**
 * Planifie l'envoi du rapport DLC quotidien. L'heure vient de la base (réglable depuis l'interface) :
 * le job est (re)créé au démarrage et à chaque enregistrement des réglages.
 */
@ApplicationScoped
public class ReportScheduler {

    /** Identité du job programmatique, utilisée aussi pour le désinscrire. */
    static final String JOB_ID = "rapport-dlc-quotidien";

    @Inject
    Scheduler scheduler;

    @Inject
    ReportSettingsService settings;

    @Inject
    DailyReportService reports;

    void onStart(@Observes StartupEvent ev) {
        reschedule();
        reports.catchUpIfMissed();
    }

    /** (Re)planifie le job depuis les réglages en base. Idempotent. */
    public synchronized void reschedule() {
        scheduler.unscheduleJob(JOB_ID);
        ReportSettings s = settings.current();
        if (!s.enabled) {
            Log.info("[RAPPORT] envoi automatique désactivé : aucun job planifié");
            return;
        }
        String cron = cronFor(s.sendTime);
        String zone = settings.zone().getId();
        scheduler.newJob(JOB_ID)
                .setCron(cron)
                .setTimeZone(zone)
                .setConcurrentExecution(ConcurrentExecution.SKIP)
                .setOverdueGracePeriod("5m")
                .setTask(exec -> reports.runScheduled())
                .schedule();
        Log.infof("[RAPPORT] envoi automatique planifié : cron=%s tz=%s", cron, zone);
    }

    /** Cron Quartz à 6 champs (quarkus.scheduler.cron-type=quartz par défaut) : « 0 30 18 ? * * ». */
    static String cronFor(LocalTime t) {
        return "0 %d %d ? * *".formatted(t.getMinute(), t.getHour());
    }
}
