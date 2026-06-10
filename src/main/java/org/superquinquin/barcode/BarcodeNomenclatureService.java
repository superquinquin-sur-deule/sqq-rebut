package org.superquinquin.barcode;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.superquinquin.odoo.OdooClient;
import org.superquinquin.odoo.OdooException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BarcodeNomenclatureService {

    private static final Duration TTL = Duration.ofHours(1);
    private static final Duration RETRY_DELAY = Duration.ofMinutes(1);
    private static final List<String> FIELDS = List.of("name", "sequence", "encoding", "type", "pattern");

    @Inject
    OdooClient odoo;

    private volatile List<CompiledRule> cache;
    private volatile Instant staleAt = Instant.MIN;

    public Optional<ScaleMatch> resolve(String scanned) {
        return ScaleBarcodeParser.match(rules(), scanned);
    }

    private List<CompiledRule> rules() {
        if (Instant.now().isBefore(staleAt)) {
            return cache;
        }
        synchronized (this) {
            if (Instant.now().isBefore(staleAt)) {
                return cache;
            }
            try {
                cache = load();
                staleAt = Instant.now().plus(TTL);
            } catch (OdooException e) {
                staleAt = Instant.now().plus(cache != null ? TTL : RETRY_DELAY);
                Log.warnf(e, "Chargement des règles barcode.rule impossible — %s",
                        cache != null ? "cache périmé conservé" : "scans balance indisponibles temporairement");
            }
            return cache != null ? cache : List.of();
        }
    }

    private List<CompiledRule> load() {
        Object domain = List.of(List.of("type", "in", List.of("weight", "price_to_weight")));
        JsonNode res = odoo.searchRead("barcode.rule", domain, FIELDS, null);
        List<CompiledRule> rules = new ArrayList<>();
        if (res != null && res.isArray()) {
            for (JsonNode r : res) {
                if (!"ean13".equals(r.path("encoding").asText())) {
                    continue;
                }
                BarcodeRuleType.fromOdoo(r.path("type").asText())
                        .flatMap(type -> ScaleBarcodeParser.compile(
                                r.path("name").asText(), r.path("sequence").asInt(), type,
                                r.path("pattern").asText(null)))
                        .ifPresentOrElse(rules::add,
                                () -> Log.warnf("Règle barcode.rule ignorée (pattern non géré) : %s — %s",
                                        r.path("name").asText(), r.path("pattern").asText()));
            }
        }
        rules.sort(Comparator.comparingInt(CompiledRule::sequence));
        Log.infof("%d règles balance chargées depuis barcode.rule", rules.size());
        return List.copyOf(rules);
    }
}
