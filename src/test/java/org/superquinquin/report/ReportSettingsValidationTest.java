package org.superquinquin.report;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportSettingsValidationTest {

    @Test
    void parsesHourMinute() {
        assertEquals(LocalTime.of(18, 0), ReportSettingsService.parseTime("18:00"));
        assertEquals(LocalTime.of(6, 5), ReportSettingsService.parseTime(" 06:05 "));
    }

    @Test
    void rejectsInvalidTime() {
        assertThrows(BadRequestException.class, () -> ReportSettingsService.parseTime("25:00"));
        assertThrows(BadRequestException.class, () -> ReportSettingsService.parseTime("18h"));
        assertThrows(BadRequestException.class, () -> ReportSettingsService.parseTime(""));
        assertThrows(BadRequestException.class, () -> ReportSettingsService.parseTime(null));
    }

    @Test
    void normalizesTrimsLowercasesAndDeduplicates() {
        List<String> mails = ReportSettingsService.normalizeRecipients(
                Arrays.asList("  Resp@Sqq.fr ", "resp@sqq.fr", "", null, "autre@sqq.fr"));
        assertEquals(List.of("resp@sqq.fr", "autre@sqq.fr"), mails);
    }

    @Test
    void rejectsMalformedAddresses() {
        assertThrows(BadRequestException.class,
                () -> ReportSettingsService.normalizeRecipients(List.of("pas-un-email")));
        assertThrows(BadRequestException.class,
                () -> ReportSettingsService.normalizeRecipients(List.of("a@b")));
    }

    @Test
    void capsRecipientCount() {
        List<String> many = IntStream.rangeClosed(0, ReportSettingsService.MAX_RECIPIENTS)
                .mapToObj(i -> "dest" + i + "@sqq.fr")
                .toList();
        assertThrows(BadRequestException.class, () -> ReportSettingsService.normalizeRecipients(many));
    }

    @Test
    void formatsQuantitiesLikeTheFrontend() {
        assertEquals("6", ReportFormat.number(6.0, 0));
        assertEquals("1,5", ReportFormat.number(1.5, 3));
        assertEquals("1,234", ReportFormat.number(1.2344, 3));
        assertEquals("1", ReportFormat.number(1.0, 3));
        assertTrue(ReportFormat.number(2.5, 3).contains(","));
    }
}
