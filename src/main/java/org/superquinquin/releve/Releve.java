package org.superquinquin.releve;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "releve")
public class Releve extends PanacheEntity {

    @Column(name = "releve_date", nullable = false, unique = true)
    public LocalDate date;
}
