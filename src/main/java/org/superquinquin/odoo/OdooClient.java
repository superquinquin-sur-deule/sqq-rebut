package org.superquinquin.odoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.List;
import java.util.Map;

/**
 * Client JSON-RPC Odoo minimal (login + execute_kw) sur {@code java.net.http}.
 * Ajoute l'en-tête HTTP Basic Auth quand {@code odoo.basic-auth.*} est renseigné (staging).
 */
@ApplicationScoped
public class OdooClient {

    private final OdooConfig config;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private volatile Integer uid;

    @Inject
    public OdooClient(OdooConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    public int uid() {
        Integer u = uid;
        if (u == null) {
            synchronized (this) {
                if (uid == null) {
                    uid = login();
                }
                u = uid;
            }
        }
        return u;
    }

    private int login() {
        ArrayNode args = mapper.createArrayNode();
        args.add(config.database());
        args.add(config.login());
        args.add(config.password());
        JsonNode res = call("common", "login", args);
        if (res == null || !res.canConvertToInt() || res.asInt() == 0) {
            throw new OdooException("Login Odoo refusé (uid=" + res + "). "
                    + "Sur le staging, vérifier aussi les identifiants HTTP Basic Auth.");
        }
        return res.asInt();
    }

    public JsonNode searchRead(String model, Object domain, List<String> fields, Integer limit) {
        ArrayNode positional = mapper.createArrayNode();
        positional.add(mapper.valueToTree(domain));
        ObjectNode kwargs = mapper.createObjectNode();
        kwargs.set("fields", mapper.valueToTree(fields));
        if (limit != null) {
            kwargs.put("limit", limit);
        }
        return executeKw(model, "search_read", positional, kwargs);
    }

    public int create(String model, Map<String, ?> values) {
        ArrayNode positional = mapper.createArrayNode();
        positional.add(mapper.valueToTree(values));
        JsonNode res = executeKw(model, "create", positional, mapper.createObjectNode());
        return res.asInt();
    }

    public JsonNode callButton(String model, List<Integer> ids, String method) {
        ArrayNode positional = mapper.createArrayNode();
        positional.add(mapper.valueToTree(ids));
        return executeKw(model, method, positional, mapper.createObjectNode());
    }

    private JsonNode executeKw(String model, String method, ArrayNode positional, ObjectNode kwargs) {
        ArrayNode args = mapper.createArrayNode();
        args.add(config.database());
        args.add(uid());
        args.add(config.password());
        args.add(model);
        args.add(method);
        args.add(positional);
        args.add(kwargs);
        return call("object", "execute_kw", args);
    }

    private JsonNode call(String service, String method, ArrayNode args) {
        ObjectNode params = mapper.createObjectNode();
        params.put("service", service);
        params.put("method", method);
        params.set("args", args);
        ObjectNode payload = mapper.createObjectNode();
        payload.put("jsonrpc", "2.0");
        payload.put("method", "call");
        payload.set("params", params);
        try {
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(config.url() + "/jsonrpc"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(40))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(payload), StandardCharsets.UTF_8));
            addBasicAuth(rb);
            HttpResponse<String> resp = http.send(rb.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new OdooException("Odoo a répondu HTTP " + resp.statusCode()
                        + " sur " + service + "." + method + " — " + snippet(resp.body()));
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode error = root.get("error");
            if (error != null && !error.isNull()) {
                throw new OdooException("Erreur Odoo (" + service + "." + method + "): " + error);
            }
            return root.get("result");
        } catch (IOException e) {
            throw new OdooException("Appel Odoo échoué (" + service + "." + method + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OdooException("Appel Odoo interrompu (" + service + "." + method + ")", e);
        }
    }

    private void addBasicAuth(HttpRequest.Builder rb) {
        String user = config.basicAuth().user();
        if (user != null && !user.isBlank()) {
            String token = Base64.getEncoder().encodeToString(
                    (user + ":" + config.basicAuth().password()).getBytes(StandardCharsets.UTF_8));
            rb.header("Authorization", "Basic " + token);
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
