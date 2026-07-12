package com.axel20378.heat_exchanger_selector.catalog.dto;

import com.axel20378.heat_exchanger_selector.catalog.domain.CatalogStatus;
import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchangerFamily;
import com.axel20378.heat_exchanger_selector.catalog.domain.RecordGranularity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class CatalogDtos {
    private CatalogDtos() {
    }

    public record CodeName(String code, String name) {
    }

    public record FamilyOption(String code, String label) {
    }

    public record ManufacturerView(Long id, long version, String name, String country, String websiteUrl) {
    }

    public record Lookups(
            List<ManufacturerView> manufacturers,
            List<FamilyOption> families,
            List<CodeName> applications,
            List<CodeName> materials
    ) {
    }

    public record SearchRequest(
            @Size(max = 100) String query,
            Set<HeatExchangerFamily> families,
            Set<Long> manufacturerIds,
            Set<String> applicationCodes,
            Set<String> materialCodes,
            @PositiveOrZero BigDecimal requiredSurfaceAreaM2,
            @PositiveOrZero BigDecimal requiredFlowM3h,
            @PositiveOrZero BigDecimal requiredPowerKw,
            BigDecimal requiredTemperatureC,
            @PositiveOrZero BigDecimal requiredPressureBar,
            @Min(0) Integer page,
            @Min(1) @Max(100) Integer size
    ) {
    }

    public record SearchItem(
            Long id,
            String slug,
            ManufacturerView manufacturer,
            String model,
            String seriesName,
            HeatExchangerFamily family,
            RecordGranularity granularity,
            String summary,
            int score,
            String confidence,
            int completeness,
            List<String> reasons,
            boolean containsMockData,
            BigDecimal surfaceAreaM2,
            BigDecimal flowMinM3h,
            BigDecimal flowMaxM3h,
            BigDecimal powerMinKw,
            BigDecimal powerMaxKw,
            BigDecimal temperatureMinC,
            BigDecimal temperatureMaxC,
            BigDecimal pressureMinBar,
            BigDecimal pressureMaxBar,
            BigDecimal widthMm,
            BigDecimal heightMm,
            BigDecimal depthMm,
            BigDecimal massKg,
            List<CodeName> applications,
            List<CodeName> materials
    ) {
    }

    public record SearchPage(
            List<SearchItem> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            long excludedUnknownCount
    ) {
    }

    public record FactView(String key, String label, String value, String unit) {
    }

    public record SourceView(String title, String url, LocalDate checkedOn, String measurementBasis) {
    }

    public record PressureLimitView(BigDecimal temperatureC, BigDecimal maxPressureBar, String note) {
    }

    public record HeatExchangerDetail(
            Long id,
            long version,
            String slug,
            ManufacturerView manufacturer,
            String model,
            String seriesName,
            HeatExchangerFamily family,
            RecordGranularity granularity,
            CatalogStatus status,
            String summary,
            BigDecimal surfaceAreaM2,
            BigDecimal flowMinM3h,
            BigDecimal flowMaxM3h,
            BigDecimal powerMinKw,
            BigDecimal powerMaxKw,
            BigDecimal temperatureMinC,
            BigDecimal temperatureMaxC,
            BigDecimal pressureMinBar,
            BigDecimal pressureMaxBar,
            BigDecimal widthMm,
            BigDecimal heightMm,
            BigDecimal depthMm,
            BigDecimal massKg,
            List<CodeName> applications,
            List<CodeName> materials,
            List<FactView> facts,
            List<SourceView> sources,
            List<PressureLimitView> pressureLimits,
            boolean containsMockData
    ) {
    }

    public record CompareRequest(
            @NotEmpty @Size(min = 2, max = 4) List<@NotNull Long> ids
    ) {
    }

    public record CompareResponse(List<HeatExchangerDetail> items) {
    }

    public record FactInput(
            @NotBlank @Size(max = 80) String key,
            @NotBlank @Size(max = 160) String label,
            @NotBlank @Size(max = 1000) String value,
            @Size(max = 40) String unit
    ) {
    }

    public record SourceInput(
            @NotBlank @Size(max = 240) String title,
            @NotBlank @Size(max = 1000) @Pattern(regexp = "https://.+", message = "должен использовать HTTPS") String url,
            @NotNull @PastOrPresent LocalDate checkedOn,
            @NotBlank @Size(max = 1000) String measurementBasis
    ) {
    }

    public record PressureLimitInput(
            @NotNull BigDecimal temperatureC,
            @NotNull @PositiveOrZero BigDecimal maxPressureBar,
            @Size(max = 500) String note
    ) {
    }

    public record CatalogRecordInput(
            Long version,
            @NotNull Long manufacturerId,
            @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "должен быть URL-safe slug") @Size(max = 160) String slug,
            @NotNull HeatExchangerFamily family,
            @NotBlank @Size(max = 160) String model,
            @Size(max = 160) String seriesName,
            @NotNull RecordGranularity granularity,
            @NotNull CatalogStatus status,
            @Size(max = 10000) String summary,
            Set<String> applicationCodes,
            Set<String> materialCodes,
            @PositiveOrZero BigDecimal surfaceAreaM2,
            @PositiveOrZero BigDecimal flowMinM3h,
            @PositiveOrZero BigDecimal flowMaxM3h,
            @PositiveOrZero BigDecimal powerMinKw,
            @PositiveOrZero BigDecimal powerMaxKw,
            BigDecimal temperatureMinC,
            BigDecimal temperatureMaxC,
            @PositiveOrZero BigDecimal pressureMinBar,
            @PositiveOrZero BigDecimal pressureMaxBar,
            @PositiveOrZero BigDecimal widthMm,
            @PositiveOrZero BigDecimal heightMm,
            @PositiveOrZero BigDecimal depthMm,
            @PositiveOrZero BigDecimal massKg,
            List<@Valid FactInput> facts,
            @NotEmpty List<@Valid SourceInput> sources,
            List<@Valid PressureLimitInput> pressureLimits
    ) {
    }

    public record StatusUpdate(@NotNull CatalogStatus status, @NotNull Long version) {
    }

    public record ManufacturerInput(
            Long version,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 80) String country,
            @Size(max = 500) String websiteUrl
    ) {
    }

    public record AdminPage(
            List<HeatExchangerDetail> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
