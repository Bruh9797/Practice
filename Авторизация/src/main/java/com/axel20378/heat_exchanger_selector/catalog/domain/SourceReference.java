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

import java.time.LocalDate;

@Entity
@Table(name = "heat_exchanger_sources")
@Getter
@Setter
@NoArgsConstructor
public class SourceReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "heat_exchanger_id", nullable = false)
    private HeatExchanger heatExchanger;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "checked_on", nullable = false)
    private LocalDate checkedOn;

    @Column(name = "measurement_basis", nullable = false, length = 1000)
    private String measurementBasis;
}
