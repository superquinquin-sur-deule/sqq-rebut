package org.superquinquin.releve;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false)
    public String barcode;

    @Column(nullable = false)
    public String name;

    public String rayon;

    public String uom;

    @Column(name = "uom_id")
    public Long uomId;

    @Column(nullable = false)
    public LocalDate dlc;

    @Column(nullable = false)
    public int qty;

    @Column(nullable = false)
    public boolean sent;

    @Column(name = "scrap_ref")
    public String scrapRef;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
