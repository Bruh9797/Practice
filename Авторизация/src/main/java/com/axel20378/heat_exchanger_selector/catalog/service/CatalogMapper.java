package com.axel20378.heat_exchanger_selector.catalog.service;

import com.axel20378.heat_exchanger_selector.catalog.domain.ApplicationArea;
import com.axel20378.heat_exchanger_selector.catalog.domain.ConstructionMaterial;
import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchanger;
import com.axel20378.heat_exchanger_selector.catalog.domain.Manufacturer;
import com.axel20378.heat_exchanger_selector.catalog.domain.PressureLimit;
import com.axel20378.heat_exchanger_selector.catalog.domain.RecordGranularity;
import com.axel20378.heat_exchanger_selector.catalog.domain.SourceReference;
import com.axel20378.heat_exchanger_selector.catalog.domain.SpecificationFact;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CodeName;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.FactView;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.HeatExchangerDetail;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.ManufacturerView;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.PressureLimitView;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SearchItem;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SourceView;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CatalogMapper {
    public ManufacturerView manufacturer(Manufacturer value) {
        return new ManufacturerView(value.getId(), value.getVersion(), value.getName(),
                value.getCountry(), value.getWebsiteUrl());
    }

    public CodeName application(ApplicationArea value) {
        return new CodeName(value.getCode(), value.getName());
    }

    public CodeName material(ConstructionMaterial value) {
        return new CodeName(value.getCode(), value.getName());
    }

    public HeatExchangerDetail detail(HeatExchanger value) {
        return new HeatExchangerDetail(
                value.getId(), value.getVersion(), value.getSlug(), manufacturer(value.getManufacturer()),
                value.getModel(), value.getSeriesName(), value.getFamily(), value.getGranularity(),
                value.getStatus(), value.getSummary(), value.getSurfaceAreaM2(), value.getFlowMinM3h(),
                value.getFlowMaxM3h(), value.getTemperatureMinC(), value.getTemperatureMaxC(),
                value.getPressureMaxBar(), value.getWidthMm(), value.getHeightMm(), value.getDepthMm(),
                value.getMassKg(),
                value.getApplications().stream().sorted(Comparator.comparing(ApplicationArea::getName))
                        .map(this::application).toList(),
                value.getMaterials().stream().sorted(Comparator.comparing(ConstructionMaterial::getName))
                        .map(this::material).toList(),
                value.getFacts().stream().map(this::fact).toList(),
                value.getSources().stream().map(this::source).toList(),
                value.getPressureLimits().stream().map(this::pressureLimit).toList());
    }

    public SearchItem searchItem(HeatExchanger value, int score, int completeness, List<String> reasons) {
        HeatExchangerDetail detail = detail(value);
        return new SearchItem(detail.id(), detail.slug(), detail.manufacturer(), detail.model(),
                detail.seriesName(), detail.family(), detail.granularity(), detail.summary(), score,
                confidenceCode(detail.granularity()), completeness, reasons, detail.surfaceAreaM2(),
                detail.flowMinM3h(), detail.flowMaxM3h(), detail.temperatureMinC(), detail.temperatureMaxC(),
                detail.pressureMaxBar(), detail.widthMm(), detail.heightMm(),
                detail.depthMm(), detail.massKg(), detail.applications(), detail.materials());
    }

    private FactView fact(SpecificationFact value) {
        return new FactView(value.getKey(), value.getLabel(), value.getValue(), value.getUnit());
    }

    private SourceView source(SourceReference value) {
        return new SourceView(value.getTitle(), value.getUrl(), value.getCheckedOn(), value.getMeasurementBasis());
    }

    private PressureLimitView pressureLimit(PressureLimit value) {
        return new PressureLimitView(value.getTemperatureC(), value.getMaxPressureBar(), value.getNote());
    }

    private String confidenceCode(RecordGranularity value) {
        return switch (value) {
            case EXACT_CONFIGURATION -> "EXACT";
            case STANDARD_MODEL -> "STANDARD_MODEL";
            case SERIES -> "SERIES_RANGE";
        };
    }
}
