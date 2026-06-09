package org.superquinquin.releve;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
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
    OdooClient odoo;

    @Inject
    OdooConfig config;

    @Transactional
    public ReleveDto view(LocalDate date) {
        LocalDate today = LocalDate.now();
        List<ReleveLineDto> dtos = ReleveLine.<ReleveLine>list("releve.date", date).stream()
                .map(l -> ReleveLineDto.from(l, today))
                .toList();
        return new ReleveDto(date, dtos);
    }

    @Transactional
    public ReleveLineDto addLine(NewLineRequest req) {
        if (req == null || req.barcode() == null || req.barcode().isBlank()) {
            throw new BadRequestException("Code-barres manquant");
        }
        if (req.dlc() == null) {
            throw new BadRequestException("DLC manquante");
        }
        if (req.qty() < 1) {
            throw new BadRequestException("Quantité invalide");
        }
        Product p = products.findByBarcode(req.barcode())
                .orElseThrow(() -> new NotFoundException("Produit inconnu pour le code-barres " + req.barcode()));

        ReleveLine l = new ReleveLine();
        l.releve = getOrCreate(LocalDate.now());
        l.productId = p.id();
        l.barcode = p.barcode() != null ? p.barcode() : req.barcode();
        l.name = p.name();
        l.rayon = p.rayon();
        l.uom = p.uom();
        l.uomId = p.uomId();
        l.dlc = req.dlc();
        l.qty = req.qty();
        l.sent = false;
        l.persist();
        return ReleveLineDto.from(l, LocalDate.now());
    }

    @Transactional
    public ReleveLineDto updateQty(Long id, int qty) {
        if (qty < 1) {
            throw new BadRequestException("Quantité invalide");
        }
        ReleveLine l = ReleveLine.findById(id);
        if (l == null) {
            throw new NotFoundException("Ligne introuvable: " + id);
        }
        l.qty = qty;
        return ReleveLineDto.from(l, LocalDate.now());
    }

    @Transactional
    public void delete(Long id) {
        ReleveLine l = ReleveLine.findById(id);
        if (l == null) {
            throw new NotFoundException("Ligne introuvable: " + id);
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
                .filter(l -> !l.sent && Urgency.of(l.dlc, today) == Urgency.J0)
                .toList();

        boolean dryRun = config.rebut().dryRun();
        List<ReleveLineDto> done = new ArrayList<>();
        int created = 0;
        for (ReleveLine l : targets) {
            if (dryRun) {
                Log.infof("[REBUT DRY-RUN] stock.scrap product_id=%s scrap_qty=%d uom=%s scrap_origin_id=%d",
                        l.productId, l.qty, l.uom, config.scrap().originId());
                l.scrapRef = "DRY-RUN";
            } else {
                int scrapId = odoo.create("stock.scrap", scrapValues(l));
                odoo.callButton("stock.scrap", List.of(scrapId), "action_validate");
                l.scrapRef = String.valueOf(scrapId);
                Log.infof("[REBUT] stock.scrap #%d validé pour %s (x%d)", scrapId, l.name, l.qty);
            }
            l.sent = true;
            created++;
            done.add(ReleveLineDto.from(l, today));
        }
        return new RebutResult(created, dryRun, done);
    }

    private Map<String, Object> scrapValues(ReleveLine l) {
        if (l.productId == null || l.uomId == null) {
            throw new BadRequestException("Ligne sans product_id/uom Odoo: " + l.id);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product_id", l.productId);
        values.put("product_uom_id", l.uomId);
        values.put("scrap_qty", (double) l.qty);
        values.put("location_id", config.scrap().locationId());
        values.put("scrap_location_id", config.scrap().scrapLocationId());
        values.put("scrap_origin_id", config.scrap().originId());
        return values;
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
