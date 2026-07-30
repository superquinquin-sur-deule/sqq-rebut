package org.superquinquin.brevo;

import java.util.List;

/**
 * Message transactionnel à envoyer.
 *
 * @param attachment contenu du fichier joint, ou {@code null} s'il n'y a rien à joindre
 */
public record BrevoMail(
        String subject,
        String html,
        List<String> to,
        String attachmentName,
        byte[] attachment) {

    public boolean hasAttachment() {
        return attachment != null && attachment.length > 0;
    }
}
