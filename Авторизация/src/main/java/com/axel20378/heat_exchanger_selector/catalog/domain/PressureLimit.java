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

import java.math.BigDecimal;

@Entity
@Table(name = "heat_exchanger_pressure_limits")
@Getter
@Setter
@NoArgsConstructor
public class PressureLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "heat_exchanger_id", nullable = false)
    private HeatExchanger heatExchanger;

    @Column(name = "temperature_c", nullable = false, precision = 10, scale = 2)
    private BigDecimal temperatureC;

    @Column(name = "max_pressure_bar", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxPressureBar;

    @Column(length = 500)
    private String note;
}
