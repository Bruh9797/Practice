package com.axel20378.heat_exchanger_selector.catalog.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "heat_exchangers")
@Getter
@Setter
@NoArgsConstructor
public class HeatExchanger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private Manufacturer manufacturer;

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HeatExchangerFamily family;

    @Column(nullable = false, length = 160)
    private String model;

    @Column(name = "series_name", length = 160)
    private String seriesName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecordGranularity granularity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CatalogStatus status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "surface_area_m2", precision = 14, scale = 3)
    private BigDecimal surfaceAreaM2;

    @Column(name = "flow_min_m3h", precision = 14, scale = 3)
    private BigDecimal flowMinM3h;

    @Column(name = "flow_max_m3h", precision = 14, scale = 3)
    private BigDecimal flowMaxM3h;

    @Column(name = "power_min_kw", precision = 14, scale = 3)
    private BigDecimal powerMinKw;

    @Column(name = "power_max_kw", precision = 14, scale = 3)
    private BigDecimal powerMaxKw;

    @Column(name = "temperature_min_c", precision = 10, scale = 2)
    private BigDecimal temperatureMinC;

    @Column(name = "temperature_max_c", precision = 10, scale = 2)
    private BigDecimal temperatureMaxC;

    @Column(name = "pressure_min_bar", precision = 10, scale = 2)
    private BigDecimal pressureMinBar;

    @Column(name = "pressure_max_bar", precision = 10, scale = 2)
    private BigDecimal pressureMaxBar;

    @Column(name = "width_mm", precision = 12, scale = 2)
    private BigDecimal widthMm;

    @Column(name = "height_mm", precision = 12, scale = 2)
    private BigDecimal heightMm;

    @Column(name = "depth_mm", precision = 12, scale = 2)
    private BigDecimal depthMm;

    @Column(name = "mass_kg", precision = 14, scale = 3)
    private BigDecimal massKg;

    @ManyToMany
    @JoinTable(name = "heat_exchanger_applications",
            joinColumns = @JoinColumn(name = "heat_exchanger_id"),
            inverseJoinColumns = @JoinColumn(name = "application_id"))
    @OrderBy("name ASC")
    private Set<ApplicationArea> applications = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "heat_exchanger_materials",
            joinColumns = @JoinColumn(name = "heat_exchanger_id"),
            inverseJoinColumns = @JoinColumn(name = "material_id"))
    @OrderBy("name ASC")
    private Set<ConstructionMaterial> materials = new LinkedHashSet<>();

    @OneToMany(mappedBy = "heatExchanger", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private Set<SpecificationFact> facts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "heatExchanger", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("checkedOn DESC, id ASC")
    private Set<SourceReference> sources = new LinkedHashSet<>();

    @OneToMany(mappedBy = "heatExchanger", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("temperatureC ASC, id ASC")
    private Set<PressureLimit> pressureLimits = new LinkedHashSet<>();

    @Version
    @Column(nullable = false)
    private long version;

    public void addFact(SpecificationFact fact) {
        fact.setHeatExchanger(this);
        facts.add(fact);
    }

    public void addSource(SourceReference source) {
        source.setHeatExchanger(this);
        sources.add(source);
    }

    public void addPressureLimit(PressureLimit limit) {
        limit.setHeatExchanger(this);
        pressureLimits.add(limit);
    }
}
