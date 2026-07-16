package com.axel20378.heat_exchanger_selector.catalog.service;

import com.axel20378.heat_exchanger_selector.catalog.domain.ApplicationArea;
import com.axel20378.heat_exchanger_selector.catalog.domain.CatalogStatus;
import com.axel20378.heat_exchanger_selector.catalog.domain.ConstructionMaterial;
import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchanger;
import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchangerFamily;
import com.axel20378.heat_exchanger_selector.catalog.domain.RecordGranularity;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CodeName;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CompareRequest;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CompareResponse;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.FamilyOption;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.HeatExchangerDetail;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.Lookups;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SearchItem;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SearchPage;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SearchRequest;
import com.axel20378.heat_exchanger_selector.catalog.exception.CatalogBadRequestException;
import com.axel20378.heat_exchanger_selector.catalog.exception.CatalogNotFoundException;
import com.axel20378.heat_exchanger_selector.catalog.repository.ApplicationAreaRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.ConstructionMaterialRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.HeatExchangerRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.ManufacturerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CatalogQueryService {
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final HeatExchangerRepository heatExchangerRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ApplicationAreaRepository applicationAreaRepository;
    private final ConstructionMaterialRepository materialRepository;
    private final CatalogMapper mapper;

    public CatalogQueryService(HeatExchangerRepository heatExchangerRepository,
                               ManufacturerRepository manufacturerRepository,
                               ApplicationAreaRepository applicationAreaRepository,
                               ConstructionMaterialRepository materialRepository,
                               CatalogMapper mapper) {
        this.heatExchangerRepository = heatExchangerRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.applicationAreaRepository = applicationAreaRepository;
        this.materialRepository = materialRepository;
        this.mapper = mapper;
    }

    public SearchPage search(SearchRequest rawRequest) {
        SearchRequest request = rawRequest == null
                ? new SearchRequest(null, Set.of(), Set.of(), Set.of(), Set.of(), null, null, null,
                null, 0, DEFAULT_PAGE_SIZE)
                : rawRequest;
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
        Set<String> requestedApplications = normalizeCodes(request.applicationCodes());
        Set<String> requestedMaterials = normalizeCodes(request.materialCodes());
        Set<String> terms = tokenize(request.query());

        List<Candidate> candidates = new ArrayList<>();
        long excludedUnknown = 0;
        for (HeatExchanger exchanger : heatExchangerRepository.findAllByStatus(CatalogStatus.PUBLISHED)) {
            if (!passesCategories(exchanger, request, requestedApplications, requestedMaterials)) {
                continue;
            }
            double textScore = textScore(exchanger, terms);
            if (!terms.isEmpty() && textScore == 0) {
                continue;
            }

            NumericEvaluation numeric = evaluateNumeric(exchanger, request);
            if (numeric.unknown()) {
                excludedUnknown++;
                continue;
            }
            if (!numeric.accepted()) {
                continue;
            }

            List<String> reasons = new ArrayList<>();
            if (!terms.isEmpty()) {
                reasons.add("Модель соответствует текстовому запросу");
            }
            if (!requestedApplications.isEmpty()) {
                reasons.add("Подходит для выбранных областей применения");
            }
            if (!requestedMaterials.isEmpty()) {
                reasons.add("Содержит все выбранные материалы");
            }
            reasons.addAll(numeric.reasons());
            double confidence = confidence(exchanger.getGranularity());
            reasons.add(confidenceReason(exchanger.getGranularity()));

            List<WeightedScore> activeGroups = new ArrayList<>();
            if (!terms.isEmpty()) {
                activeGroups.add(new WeightedScore(30, textScore));
            }
            if (!requestedApplications.isEmpty()) {
                activeGroups.add(new WeightedScore(20, 1d));
            }
            if (!requestedMaterials.isEmpty()) {
                activeGroups.add(new WeightedScore(10, 1d));
            }
            if (numeric.active()) {
                activeGroups.add(new WeightedScore(30, numeric.score()));
            }
            activeGroups.add(new WeightedScore(10, confidence));
            int score = normalizedScore(activeGroups);
            int completeness = completeness(exchanger);
            candidates.add(new Candidate(exchanger, score, completeness, List.copyOf(reasons)));
        }

        candidates.sort(Comparator.comparingInt(Candidate::score).reversed()
                .thenComparing(Comparator.comparingInt(Candidate::completeness).reversed())
                .thenComparing(candidate -> candidate.exchanger().getManufacturer().getName(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(candidate -> candidate.exchanger().getModel(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(candidate -> candidate.exchanger().getId()));

        long total = candidates.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, candidates.size());
        int to = Math.min(from + size, candidates.size());
        List<SearchItem> items = candidates.subList(from, to).stream()
                .map(candidate -> mapper.searchItem(candidate.exchanger(), candidate.score(),
                        candidate.completeness(), candidate.reasons()))
                .toList();
        return new SearchPage(items, page, size, total, totalPages, excludedUnknown);
    }

    public HeatExchangerDetail detail(String slug) {
        HeatExchanger exchanger = heatExchangerRepository.findOneBySlugAndStatus(slug, CatalogStatus.PUBLISHED)
                .orElseThrow(() -> new CatalogNotFoundException("Опубликованный теплообменник '" + slug + "' не найден"));
        return mapper.detail(exchanger);
    }

    public CompareResponse compare(CompareRequest request) {
        if (new HashSet<>(request.ids()).size() != request.ids().size()) {
            throw new CatalogBadRequestException("Для сравнения укажите разные аппараты");
        }
        List<HeatExchanger> found = heatExchangerRepository.findAllByIdInAndStatus(request.ids(), CatalogStatus.PUBLISHED);
        Map<Long, HeatExchanger> byId = found.stream().collect(Collectors.toMap(HeatExchanger::getId, Function.identity()));
        List<Long> missing = request.ids().stream().filter(id -> !byId.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new CatalogNotFoundException("Не найдены опубликованные аппараты: " + missing);
        }
        return new CompareResponse(request.ids().stream().map(byId::get).map(mapper::detail).toList());
    }

    public Lookups lookups() {
        return new Lookups(
                manufacturerRepository.findAllByOrderByNameAsc().stream().map(mapper::manufacturer).toList(),
                List.of(HeatExchangerFamily.values()).stream()
                        .map(value -> new FamilyOption(value.name(), value.getLabel())).toList(),
                applicationAreaRepository.findAllByOrderByNameAsc().stream().map(mapper::application).toList(),
                materialRepository.findAllByOrderByNameAsc().stream().map(mapper::material).toList());
    }

    private boolean passesCategories(HeatExchanger exchanger, SearchRequest request,
                                     Set<String> requestedApplications, Set<String> requestedMaterials) {
        if (request.families() != null && !request.families().isEmpty()
                && !request.families().contains(exchanger.getFamily())) {
            return false;
        }
        if (request.manufacturerIds() != null && !request.manufacturerIds().isEmpty()
                && !request.manufacturerIds().contains(exchanger.getManufacturer().getId())) {
            return false;
        }
        Set<String> actualApplications = exchanger.getApplications().stream()
                .map(ApplicationArea::getCode).collect(Collectors.toSet());
        if (!actualApplications.containsAll(requestedApplications)) {
            return false;
        }
        Set<String> actualMaterials = exchanger.getMaterials().stream()
                .map(ConstructionMaterial::getCode).collect(Collectors.toSet());
        return actualMaterials.containsAll(requestedMaterials);
    }

    private double textScore(HeatExchanger exchanger, Set<String> terms) {
        if (terms.isEmpty()) {
            return 1;
        }
        String searchable = String.join(" ",
                exchanger.getManufacturer().getName(),
                exchanger.getModel(),
                Objects.toString(exchanger.getSeriesName(), ""),
                Objects.toString(exchanger.getSummary(), ""),
                exchanger.getApplications().stream().map(ApplicationArea::getName).collect(Collectors.joining(" ")),
                exchanger.getMaterials().stream().map(ConstructionMaterial::getName).collect(Collectors.joining(" ")),
                exchanger.getFacts().stream().map(fact -> fact.getLabel() + " " + fact.getValue())
                        .collect(Collectors.joining(" "))).toLowerCase(Locale.ROOT);
        long matched = terms.stream().filter(searchable::contains).count();
        return (double) matched / terms.size();
    }

    private NumericEvaluation evaluateNumeric(HeatExchanger exchanger, SearchRequest request) {
        List<Double> scores = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        boolean active = false;

        if (request.requiredSurfaceAreaM2() != null) {
            active = true;
            if (exchanger.getSurfaceAreaM2() == null) {
                return NumericEvaluation.unknownResult();
            }
            if (exchanger.getSurfaceAreaM2().compareTo(request.requiredSurfaceAreaM2()) < 0) {
                return NumericEvaluation.rejected();
            }
            scores.add(capacityCloseness(request.requiredSurfaceAreaM2(), exchanger.getSurfaceAreaM2()));
            reasons.add("Площадь поверхности покрывает требование: " + exchanger.getSurfaceAreaM2() + " м²");
        }
        if (request.requiredFlowM3h() != null) {
            active = true;
            if (exchanger.getFlowMinM3h() == null || exchanger.getFlowMaxM3h() == null) {
                return NumericEvaluation.unknownResult();
            }
            if (!inside(request.requiredFlowM3h(), exchanger.getFlowMinM3h(), exchanger.getFlowMaxM3h())) {
                return NumericEvaluation.rejected();
            }
            scores.add(rangeCloseness(request.requiredFlowM3h(), exchanger.getFlowMinM3h(), exchanger.getFlowMaxM3h()));
            reasons.add("Требуемый расход входит в диапазон " + exchanger.getFlowMinM3h() + "–"
                    + exchanger.getFlowMaxM3h() + " м³/ч");
        }
        if (request.requiredTemperatureC() != null) {
            active = true;
            if (exchanger.getTemperatureMinC() == null || exchanger.getTemperatureMaxC() == null) {
                return NumericEvaluation.unknownResult();
            }
            if (!inside(request.requiredTemperatureC(), exchanger.getTemperatureMinC(), exchanger.getTemperatureMaxC())) {
                return NumericEvaluation.rejected();
            }
            scores.add(rangeCloseness(request.requiredTemperatureC(), exchanger.getTemperatureMinC(),
                    exchanger.getTemperatureMaxC()));
            reasons.add("Рабочая температура входит в допустимый диапазон");
        }
        if (request.requiredPressureBar() != null) {
            active = true;
            if (exchanger.getPressureMaxBar() == null) {
                return NumericEvaluation.unknownResult();
            }
            if (exchanger.getPressureMaxBar().compareTo(request.requiredPressureBar()) < 0) {
                return NumericEvaluation.rejected();
            }
            scores.add(capacityCloseness(request.requiredPressureBar(), exchanger.getPressureMaxBar()));
            reasons.add("Максимальное давление покрывает требование: " + exchanger.getPressureMaxBar() + " бар");
        }
        double score = scores.isEmpty() ? 1 : scores.stream().mapToDouble(Double::doubleValue).average().orElse(1);
        return new NumericEvaluation(true, false, active, score, List.copyOf(reasons));
    }

    private static boolean inside(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    private static double capacityCloseness(BigDecimal requested, BigDecimal available) {
        if (requested.signum() == 0 || available.signum() == 0) {
            return 1;
        }
        return requested.divide(available, 8, RoundingMode.HALF_UP).doubleValue();
    }

    private static double rangeCloseness(BigDecimal requested, BigDecimal min, BigDecimal max) {
        BigDecimal span = max.subtract(min);
        if (span.signum() == 0) {
            return 1;
        }
        BigDecimal midpoint = min.add(max).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
        BigDecimal normalizedDistance = requested.subtract(midpoint).abs()
                .divide(span.divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP), 8, RoundingMode.HALF_UP);
        return Math.max(0.5, 1 - normalizedDistance.doubleValue() * 0.5);
    }

    private static int normalizedScore(List<WeightedScore> activeGroups) {
        int totalWeight = activeGroups.stream().mapToInt(WeightedScore::weight).sum();
        double weighted = activeGroups.stream()
                .mapToDouble(entry -> entry.weight() * entry.score()).sum();
        return (int) Math.round(weighted / totalWeight * 100);
    }

    private static double confidence(RecordGranularity granularity) {
        return switch (granularity) {
            case EXACT_CONFIGURATION -> 1;
            case STANDARD_MODEL -> 0.8;
            case SERIES -> 0.55;
        };
    }

    private static String confidenceReason(RecordGranularity granularity) {
        return switch (granularity) {
            case EXACT_CONFIGURATION -> "Параметры относятся к точной конфигурации";
            case STANDARD_MODEL -> "Параметры относятся к стандартной модели";
            case SERIES -> "Данные относятся к серии — конфигурацию необходимо уточнить у производителя";
        };
    }

    private static int completeness(HeatExchanger value) {
        int known = 0;
        Object[] numeric = {
                value.getSurfaceAreaM2(), value.getFlowMinM3h(), value.getFlowMaxM3h(),
                value.getTemperatureMinC(), value.getTemperatureMaxC(), value.getPressureMaxBar(),
                value.getWidthMm(), value.getHeightMm(), value.getDepthMm(), value.getMassKg()
        };
        for (Object field : numeric) {
            known += field == null ? 0 : 1;
        }
        known += value.getApplications().isEmpty() ? 0 : 1;
        known += value.getMaterials().isEmpty() ? 0 : 1;
        known += value.getFacts().isEmpty() ? 0 : 1;
        known += value.getSources().isEmpty() ? 0 : 1;
        return (int) Math.round(known / 17d * 100);
    }

    private static Set<String> normalizeCodes(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    private static Set<String> tokenize(String query) {
        if (query == null || query.isBlank()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (String term : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!term.isBlank()) {
                result.add(term);
            }
        }
        return result;
    }

    private record Candidate(HeatExchanger exchanger, int score, int completeness, List<String> reasons) {
    }

    private record WeightedScore(int weight, double score) {
    }

    private record NumericEvaluation(boolean accepted, boolean unknown, boolean active, double score,
                                     List<String> reasons) {
        private static NumericEvaluation unknownResult() {
            return new NumericEvaluation(false, true, true, 0, List.of());
        }

        private static NumericEvaluation rejected() {
            return new NumericEvaluation(false, false, true, 0, List.of());
        }
    }
}
