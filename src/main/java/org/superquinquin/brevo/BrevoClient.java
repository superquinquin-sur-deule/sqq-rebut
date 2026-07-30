package org.superquinquin.brevo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Client de l'API transactionnelle Brevo (POST /v3/smtp/email) sur {@code java.net.http},
 * calqué sur {@link org.superquinquin.odoo.OdooClient}.
 */
@ApplicationScoped
public class BrevoClient {

    private final BrevoConfig config;
    private final ObjectMapper mapper;
    private final HttpClient http;

    @Inject
    public BrevoClient(BrevoConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    /**
     * Envoie le message.
     *
     * @return l'identifiant de message renvoyé par Brevo
     */
    public String send(BrevoMail mail) {
        String key = config.apiKey()
                .filter(k -> !k.isBlank())
                .orElseThrow(() -> new BrevoException("Clé API Brevo absente (BREVO_API_KEY)"));
        if (mail.to() == null || mail.to().isEmpty()) {
            throw new BrevoException("Aucun destinataire configuré");
        }

        String payload = payload(mail);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/v3/smtp/email"))
                    .header("api-key", key)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(40))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                throw new BrevoException("Brevo a répondu HTTP " + resp.statusCode()
                        + " — " + snippet(resp.body()));
            }
            String messageId = mapper.readTree(resp.body()).path("messageId").asText(null);
            // On ne logue jamais la clé API ni le payload (il contient le base64 de la pièce jointe).
            Log.infof("[RAPPORT] e-mail envoyé à %s (messageId=%s)", mail.to(), messageId);
            return messageId;
        } catch (IOException e) {
            throw new BrevoException("Appel Brevo échoué", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrevoException("Appel Brevo interrompu", e);
        }
    }

    private String payload(BrevoMail mail) {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode sender = payload.putObject("sender");
        sender.put("name", config.sender().name());
        sender.put("email", config.sender().email());
        ArrayNode to = payload.putArray("to");
        mail.to().forEach(m -> to.addObject().put("email", m));
        payload.put("subject", mail.subject());
        payload.put("htmlContent", mail.html());
        // Un tableau « attachment » vide est refusé par Brevo (400) : on omet la clé.
        if (mail.hasAttachment()) {
            ObjectNode att = payload.putArray("attachment").addObject();
            att.put("content", Base64.getEncoder().encodeToString(mail.attachment()));
            att.put("name", mail.attachmentName());
        }
        try {
            return mapper.writeValueAsString(payload);
        } catch (IOException e) {
            throw new BrevoException("Sérialisation du message Brevo échouée", e);
        }
    }

    private static String snippet(String body) {
        if (body == null) {
            return "(vide)";
        }
        String s = body.strip();
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
