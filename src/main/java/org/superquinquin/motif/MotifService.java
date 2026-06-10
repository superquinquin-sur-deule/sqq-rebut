package org.superquinquin.motif;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.superquinquin.odoo.OdooClient;
import org.superquinquin.odoo.OdooConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Motifs de rupture lus depuis Odoo {@code stock.scrap.origin}, mis en cache pour la durée de vie de l'app
 * (même schéma de cache paresseux que {@link OdooClient#uid()}). On exclut l'origine réservée au flux DLC
 * ({@code odoo.scrap.origin-id}, « DLC Dépassée »).
 */
@ApplicationScoped
public class MotifService {

    @Inject
    OdooClient odoo;

    @Inject
    OdooConfig config;

    private volatile List<Motif> cache;

    public List<Motif> list() {
        List<Motif> c = cache;
        if (c == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = fetch();
                }
                c = cache;
            }
        }
        return c;
    }

    public Optional<Motif> byId(long id) {
        return list().stream().filter(m -> m.id() == id).findFirst();
    }

    private List<Motif> fetch() {
        Object domain = List.of(List.of("id", "!=", config.scrap().originId()));
        JsonNode res = odoo.searchRead("stock.scrap.origin", domain, List.of("id", "name"), null);
        List<Motif> motifs = new ArrayList<>();
        if (res != null && res.isArray()) {
            for (JsonNode n : res) {
                motifs.add(new Motif(n.path("id").asLong(), n.path("name").asText("")));
            }
        }
        return List.copyOf(motifs);
    }
}
