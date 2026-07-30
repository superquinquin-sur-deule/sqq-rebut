package org.superquinquin.report;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportSchedulerTest {

    @Test
    void cronFollowsQuartzSixFieldSyntax() {
        // quarkus.scheduler.cron-type=quartz : 6 champs, « ? » obligatoire sur dom ou dow.
        assertEquals("0 30 18 ? * *", ReportScheduler.cronFor(LocalTime.of(18, 30)));
        assertEquals("0 5 6 ? * *", ReportScheduler.cronFor(LocalTime.of(6, 5)));
        assertEquals("0 0 0 ? * *", ReportScheduler.cronFor(LocalTime.MIDNIGHT));
        // Les secondes de l'heure réglée sont ignorées : l'UI ne saisit que HH:mm.
        assertEquals("0 45 23 ? * *", ReportScheduler.cronFor(LocalTime.of(23, 45, 59)));
    }
}
