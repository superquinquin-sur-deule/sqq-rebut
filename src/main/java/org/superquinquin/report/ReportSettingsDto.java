package org.superquinquin.report;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Réglages du rapport quotidien tels qu'exposés à l'interface.
 * {@code sendTime} est une chaîne « HH:mm » : c'est exactement la valeur d'un {@code <input type="time">}.
 * Les champs de suivi du dernier envoi sont en lecture seule (ignorés en écriture).
 */
@Schema(requiredProperties = {"enabled", "sendTime", "recipients", "thresholdPieces", "thresholdKg"})
public record ReportSettingsDto(
        boolean enabled,
        @Schema(description = "Heure d'envoi (Europe/Paris), format HH:mm", example = "18:00")
        String sendTime,
        @Schema(description = "Adresses e-mail destinataires")
        List<String> recipients,
        @Schema(description = "Seuil en pièces : au-delà (strictement), le produit est listé")
        double thresholdPieces,
        @Schema(description = "Seuil en kg : au-delà (strictement), le produit est listé")
        double thresholdKg,
        @Schema(readOnly = true, description = "Date/heure du dernier envoi (automatique ou manuel)")
        Instant lastSentAt,
        @Schema(readOnly = true, description = "Statut du dernier envoi : OK ou ERREUR")
        String lastStatus,
        @Schema(readOnly = true, description = "Message d'erreur du dernier envoi")
        String lastError) {
}
