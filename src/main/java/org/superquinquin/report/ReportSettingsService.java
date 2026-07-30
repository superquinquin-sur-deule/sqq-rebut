package org.superquinquin.report;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Lecture, validation et mise à jour des réglages du rapport quotidien (ligne unique en base). */
@ApplicationScoped
public class ReportSettingsService {

    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERREUR";

    /** Garde-fou : un rapport ne s'envoie pas à une liste de diffusion. */
    static final int MAX_RECIPIENTS = 20;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_ERROR_LENGTH = 2000;

    @ConfigProperty(name = "report.timezone")
    String timezone;

    /**
     * Fuseau de référence du rapport. Le conteneur tourne en UTC : sans cela, « aujourd'hui »
     * bascule à 2 h du matin heure de Paris.
     */
    public ZoneId zone() {
        return ZoneId.of(timezone);
    }

    public LocalDate today() {
        return LocalDate.now(zone());
    }

    @Transactional
    public ReportSettings current() {
        ReportSettings s = ReportSettings.findById(ReportSettings.ID);
        if (s == null) {
            // Filet de sécurité si la V6 a été jouée sans son insert.
            s = new ReportSettings();
            s.persist();
        }
        return s;
    }

    @Transactional
    public ReportSettingsDto get() {
        return toDto(current());
    }

    @Transactional
    public ReportSettingsDto update(ReportSettingsDto req) {
        if (req == null) {
            throw new BadRequestException("Réglages manquants");
        }
        LocalTime time = parseTime(req.sendTime());
        List<String> mails = normalizeRecipients(req.recipients());
        if (req.enabled() && mails.isEmpty()) {
            throw new BadRequestException("Au moins un destinataire est requis pour activer l'envoi");
        }
        double pieces = validThreshold(req.thresholdPieces(), "Seuil en pièces invalide");
        double kg = validThreshold(req.thresholdKg(), "Seuil en kg invalide");

        ReportSettings s = current();
        s.enabled = req.enabled();
        s.sendTime = time;
        s.recipients = String.join(",", mails);
        s.thresholdPieces = pieces;
        s.thresholdKg = kg;
        s.updatedAt = Instant.now();
        return toDto(s);
    }

    /**
     * Réserve l'envoi automatique du jour.
     *
     * @return {@code false} si le rapport a déjà été envoyé automatiquement aujourd'hui.
     */
    @Transactional
    public boolean claimToday(LocalDate day) {
        ReportSettings s = ReportSettings.findById(ReportSettings.ID, LockModeType.PESSIMISTIC_WRITE);
        if (s == null) {
            s = current();
        }
        if (day.equals(s.lastSentDate)) {
            return false;
        }
        s.lastSentDate = day;
        return true;
    }

    @Transactional
    public void recordResult(String status, String error) {
        ReportSettings s = current();
        s.lastSentAt = Instant.now();
        s.lastStatus = status;
        s.lastError = error == null ? null
                : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
    }

    public static List<String> recipientsOf(ReportSettings s) {
        if (s.recipients == null || s.recipients.isBlank()) {
            return List.of();
        }
        return Arrays.stream(s.recipients.split(","))
                .map(String::strip)
                .filter(m -> !m.isBlank())
                .toList();
    }

    static ReportSettingsDto toDto(ReportSettings s) {
        return new ReportSettingsDto(
                s.enabled,
                s.sendTime.format(HH_MM),
                recipientsOf(s),
                s.thresholdPieces,
                s.thresholdKg,
                s.lastSentAt,
                s.lastStatus,
                s.lastError);
    }

    static LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Heure d'envoi manquante (attendu HH:mm)");
        }
        try {
            return LocalTime.parse(raw.strip());
        } catch (DateTimeException e) {
            throw new BadRequestException("Heure d'envoi invalide (attendu HH:mm): " + raw);
        }
    }

    /** Normalise, dédoublonne et valide les adresses ; l'ordre de saisie est conservé. */
    static List<String> normalizeRecipients(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String r : raw) {
            if (r == null || r.isBlank()) {
                continue;
            }
            String mail = r.strip().toLowerCase(Locale.ROOT);
            if (!EMAIL.matcher(mail).matches()) {
                throw new BadRequestException("Adresse e-mail invalide: " + r.strip());
            }
            if (!out.contains(mail)) {
                out.add(mail);
            }
        }
        if (out.size() > MAX_RECIPIENTS) {
            throw new BadRequestException("Trop de destinataires (maximum " + MAX_RECIPIENTS + ")");
        }
        return out;
    }

    private static double validThreshold(double value, String message) {
        if (!Double.isFinite(value) || value < 0) {
            throw new BadRequestException(message);
        }
        return value;
    }
}
