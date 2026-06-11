package org.superquinquin.releve;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.superquinquin.motif.Motif;
import org.superquinquin.motif.MotifService;
import org.superquinquin.odoo.OdooClient;
import org.superquinquin.odoo.OdooConfig;
import org.superquinquin.product.Product;
import org.superquinquin.product.ProductService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ReleveService {

    @Inject
    ProductService products;

    @Inject
    MotifService motifs;

    @Inject
    OdooClient odoo;

    @Inject
    OdooConfig config;

    @Transactional
    public ReleveDto view(LocalDate date) {
        Releve r = Releve.find("date", date).firstResult();
        List<ReleveLineDto> dtos = ReleveLine.<ReleveLine>list("releve.date", date).stream()
                .map(l -> ReleveLineDto.from(l, date))
                .toList();
        return new ReleveDto(r != null ? r.id : null, date, dtos);
    }

    @Transactional
    public ReleveDto viewById(Long id) {
        Releve r = Releve.findById(id);
        if (r == null) {
            throw new NotFoundException("Relevé introuvable: " + id);
        }
        List<ReleveLineDto> dtos = ReleveLine.<ReleveLine>list("releve", r).stream()
                .map(l -> ReleveLineDto.from(l, r.date))
                .toList();
        return new ReleveDto(r.id, r.date, dtos);
    }

    @Transactional
    public List<ReleveSummaryDto> listSummaries() {
        return Releve.<Releve>list("order by date desc").stream()
                .map(r -> new ReleveSummaryDto(r.id, r.date, ReleveLine.count("releve", r)))
                .toList();
    }

    @Transactional
    public ReleveLineDto addLine(NewLineRequest req) {
        if (req == null || req.barcode() == null || req.barcode().isBlank()) {
            throw new BadRequestException("Code-barres manquant");
        }
        if (!Double.isFinite(req.qty()) || req.qty() <= 0) {
            throw new BadRequestException("Quantité invalide");
        }
        LineType type = parseType(req.type());
        Motif motif = null;
        if (type == LineType.DLC) {
            if (req.dlc() == null) {
                throw new BadRequestException("DLC manquante");
            }
        } else {
            if (req.motifId() == null) {
                throw new BadRequestException("Motif manquant");
            }
            motif = motifs.byId(req.motifId())
                    .orElseThrow(() -> new BadRequestException("Motif inconnu: " + req.motifId()));
        }

        Product p = products.findByBarcode(req.barcode())
                .orElseThrow(() -> new NotFoundException("Produit inconnu pour le code-barres " + req.barcode()));

        Releve releve = getOrCreate(LocalDate.now());
        String barcode = p.barcode() != null ? p.barcode() : req.barcode();

        if (type == LineType.DLC) {
            ReleveLine existing = ReleveLine.find(
                    "releve = ?1 and barcode = ?2 and type = ?3 and dlc = ?4 and sent = false",
                    releve, barcode, LineType.DLC, req.dlc()).firstResult();
            if (existing != null) {
                existing.qty = round3(existing.qty + req.qty());
                return ReleveLineDto.from(existing, LocalDate.now());
            }
        }

        ReleveLine l = new ReleveLine();
        l.releve = releve;
        l.productId = p.id();
        l.barcode = barcode;
        l.name = p.name();
        l.rayon = p.rayon();
        l.uom = p.uom();
        l.uomId = p.uomId();
        l.type = type;
        if (type == LineType.DLC) {
            l.dlc = req.dlc();
        } else {
            l.motifId = motif.id();
            l.motifLabel = motif.label();
        }
        l.qty = round3(req.qty());
        l.sent = false;
        l.persist();
        if (type == LineType.PERTE) {
            scrapLine(l);
        }
        return ReleveLineDto.from(l, LocalDate.now());
    }

    private static LineType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return LineType.DLC;
        }
        try {
            return LineType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Type de ligne invalide: " + raw);
        }
    }

    @Transactional
    public ReleveLineDto updateLine(Long id, Double qty) {
        ReleveLine l = ReleveLine.findById(id);
        if (l == null) {
            throw new NotFoundException("Ligne introuvable: " + id);
        }
        if (l.sent) {
            throw new BadRequestException("Ligne déjà envoyée au rebut: " + id);
        }
        if (qty != null) {
            if (!Double.isFinite(qty) || qty <= 0) {
                throw new BadRequestException("Quantité invalide");
            }
            l.qty = round3(qty);
        }
        return ReleveLineDto.from(l, LocalDate.now());
    }

    /** Arrondi au gramme : évite les artefacts binaires en cumulant des poids (1.234 + 0.5). */
    private static double round3(double qty) {
        return Math.round(qty * 1000) / 1000.0;
    }

    @Transactional
    public void delete(Long id) {
        ReleveLine l = ReleveLine.findById(id);
        if (l == null) {
            throw new NotFoundException("Ligne introuvable: " + id);
        }
        if (l.sent) {
            throw new BadRequestException("Ligne déjà envoyée au rebut: " + id);
        }
        l.delete();
    }

    @Transactional
    public RebutResult sendRebut(List<Long> ids) {
        LocalDate today = LocalDate.now();
        List<ReleveLine> candidates = (ids != null && !ids.isEmpty())
                ? ReleveLine.list("id in ?1", ids)
                : ReleveLine.list("releve.date", today);
        List<ReleveLine> targets = candidates.stream()
                .filter(l -> !l.sent && l.type == LineType.DLC && Urgency.of(l.dlc, today) == Urgency.J0)
                .toList();

        List<ReleveLineDto> done = new ArrayList<>();
        for (ReleveLine l : targets) {
            scrapLine(l);
            done.add(ReleveLineDto.from(l, today));
        }
        return new RebutResult(done.size(), config.rebut().dryRun(), done);
    }

    /** Crée et valide le stock.scrap dans Odoo (ou log seulement en dry-run), puis marque la ligne envoyée. */
    private void scrapLine(ReleveLine l) {
        if (config.rebut().dryRun()) {
            Log.infof("[REBUT DRY-RUN] stock.scrap product_id=%s scrap_qty=%s uom=%s scrap_origin_id=%d",
                    l.productId, l.qty, l.uom, scrapOriginId(l));
            l.scrapRef = "DRY-RUN";
        } else {
            int scrapId = odoo.create("stock.scrap", scrapValues(l));
            odoo.callButton("stock.scrap", List.of(scrapId), "action_validate");
            l.scrapRef = String.valueOf(scrapId);
            Log.infof("[REBUT] stock.scrap #%d validé pour %s (x%s)", scrapId, l.name, l.qty);
        }
        l.sent = true;
    }

    private Map<String, Object> scrapValues(ReleveLine l) {
        if (l.productId == null || l.uomId == null) {
            throw new BadRequestException("Ligne sans product_id/uom Odoo: " + l.id);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product_id", l.productId);
        values.put("product_uom_id", l.uomId);
        values.put("scrap_qty", l.qty);
        values.put("location_id", config.scrap().locationId());
        values.put("scrap_location_id", config.scrap().scrapLocationId());
        values.put("scrap_origin_id", scrapOriginId(l));
        return values;
    }

    /** Origine du scrap : le motif choisi pour une perte, sinon « DLC Dépassée » (config) pour une DLC. */
    private long scrapOriginId(ReleveLine l) {
        if (l.type == LineType.PERTE && l.motifId != null) {
            return l.motifId;
        }
        return config.scrap().originId();
    }

    @Transactional
    public Releve getOrCreate(LocalDate date) {
        Releve r = Releve.find("date", date).firstResult();
        if (r == null) {
            r = new Releve();
            r.date = date;
            r.persist();
        }
        return r;
    }
}
