package org.superquinquin.report;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/** Compte rendu d'un envoi de rapport (bouton « Envoyer maintenant »). */
@Schema(requiredProperties = {"lineCount", "recipients"})
public record ReportSendResult(
        @Schema(description = "Nombre de produits au-delà des seuils dans le rapport envoyé")
        int lineCount,
        List<String> recipients,
        @Schema(description = "Identifiant du message renvoyé par Brevo")
        String messageId) {
}
