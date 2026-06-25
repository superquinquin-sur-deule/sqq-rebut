package org.superquinquin.releve;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "releve_line")
public class ReleveLine extends PanacheEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "releve_id")
    public Releve releve;

    @Column(name = "product_id")
    public Long productId;

    public String barcode;

    @Column(nullable = false)
    public String name;

    public String rayon;

    public String uom;

    @Column(name = "uom_id")
    public Long uomId;

    @Column(name = "qty_available")
    public Double qtyAvailable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public LineType type = LineType.DLC;

    public LocalDate dlc;

    @Column(name = "motif_id")
    public Long motifId;

    @Column(name = "motif_label")
    public String motifLabel;

    @Column(nullable = false)
    public double qty;

    @Column(nullable = false)
    public boolean sent;

    @Column(name = "scrap_ref")
    public String scrapRef;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
