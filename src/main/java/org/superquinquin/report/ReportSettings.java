package org.superquinquin.report;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Réglages du rapport DLC quotidien : une seule ligne (id = 1), créée par la migration V6. */
@Entity
@Table(name = "report_settings")
public class ReportSettings extends PanacheEntityBase {

    public static final long ID = 1L;

    @Id
    public Long id = ID;

    @Column(nullable = false)
    public boolean enabled;

    @Column(name = "send_time", nullable = false)
    public LocalTime sendTime = LocalTime.of(18, 0);

    /** Destinataires séparés par des virgules : 2 à 5 adresses ne justifient pas une table dédiée. */
    @Column(nullable = false, length = 2000)
    public String recipients = "";

    @Column(name = "threshold_pieces", nullable = false)
    public double thresholdPieces = 5;

    @Column(name = "threshold_kg", nullable = false)
    public double thresholdKg = 1;

    /** Journée déjà consommée par l'envoi automatique (anti double-envoi au redémarrage). */
    @Column(name = "last_sent_date")
    public LocalDate lastSentDate;

    @Column(name = "last_sent_at")
    public Instant lastSentAt;

    /** {@link ReportSettingsService#STATUS_OK} ou {@link ReportSettingsService#STATUS_ERROR}. */
    @Column(name = "last_status")
    public String lastStatus;

    @Column(name = "last_error", length = 2000)
    public String lastError;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
