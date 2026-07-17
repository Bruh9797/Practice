package com.axel20378.heat_exchanger_selector.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "heat_exchanger_facts")
@Getter
@Setter
@NoArgsConstructor
public class SpecificationFact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "heat_exchanger_id", nullable = false)
    private HeatExchanger heatExchanger;

    @Column(name = "fact_key", nullable = false, length = 80)
    private String key;

    @Column(nullable = false, length = 160)
    private String label;

    @Column(name = "fact_value", nullable = false, length = 1000)
    private String value;

    @Column(length = 40)
    private String unit;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
