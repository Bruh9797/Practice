package com.axel20378.heat_exchanger_selector.catalog.service;

import com.axel20378.heat_exchanger_selector.catalog.domain.ApplicationArea;
import com.axel20378.heat_exchanger_selector.catalog.domain.CatalogStatus;
import com.axel20378.heat_exchanger_selector.catalog.domain.ConstructionMaterial;
import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchanger;
import com.axel20378.heat_exchanger_selector.catalog.domain.Manufacturer;
import com.axel20378.heat_exchanger_selector.catalog.domain.PressureLimit;
import com.axel20378.heat_exchanger_selector.catalog.domain.SourceReference;
import com.axel20378.heat_exchanger_selector.catalog.domain.SpecificationFact;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.AdminPage;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CatalogRecordInput;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.FactInput;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.HeatExchangerDetail;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.ManufacturerInput;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.ManufacturerView;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.PressureLimitInput;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SourceInput;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.StatusUpdate;
import com.axel20378.heat_exchanger_selector.catalog.exception.CatalogBadRequestException;
import com.axel20378.heat_exchanger_selector.catalog.exception.CatalogConflictException;
import com.axel20378.heat_exchanger_selector.catalog.exception.CatalogNotFoundException;
import com.axel20378.heat_exchanger_selector.catalog.repository.ApplicationAreaRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.ConstructionMaterialRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.HeatExchangerRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.ManufacturerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogAdminService {
    private final HeatExchangerRepository heatExchangerRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ApplicationAreaRepository applicationAreaRepository;
    private final ConstructionMaterialRepository materialRepository;
    private final CatalogMapper mapper;

    public CatalogAdminService(HeatExchangerRepository heatExchangerRepository,
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

    @Transactional(readOnly = true)
    public AdminPage list(String query, CatalogStatus status, int page, int size) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<HeatExchanger> filtered = heatExchangerRepository.findAllWithDetails().stream()
                .filter(value -> status == null || value.getStatus() == status)
                .filter(value -> normalizedQuery.isBlank()
                        || value.getModel().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || value.getSlug().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || value.getManufacturer().getName().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(HeatExchanger::getStatus)
                        .thenComparing(value -> value.getManufacturer().getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(HeatExchanger::getModel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(HeatExchanger::getId))
                .toList();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        int pages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / size);
        return new AdminPage(filtered.subList(from, to).stream().map(mapper::detail).toList(),
                page, size, filtered.size(), pages);
    }

    @Transactional(readOnly = true)
    public HeatExchangerDetail get(Long id) {
        return mapper.detail(findExchanger(id));
    }

    public HeatExchangerDetail create(CatalogRecordInput input) {
        if (heatExchangerRepository.existsBySlug(input.slug())) {
            throw new CatalogConflictException("Slug '" + input.slug() + "' уже используется");
        }
        HeatExchanger exchanger = new HeatExchanger();
        apply(exchanger, input);
        return mapper.detail(heatExchangerRepository.saveAndFlush(exchanger));
    }

    public HeatExchangerDetail update(Long id, CatalogRecordInput input) {
        HeatExchanger exchanger = findExchanger(id);
        requireVersion(input.version(), exchanger.getVersion());
        if (heatExchangerRepository.existsBySlugAndIdNot(input.slug(), id)) {
            throw new CatalogConflictException("Slug '" + input.slug() + "' уже используется");
        }
        apply(exchanger, input);
        return mapper.detail(heatExchangerRepository.saveAndFlush(exchanger));
    }

    public HeatExchangerDetail updateStatus(Long id, StatusUpdate update) {
        HeatExchanger exchanger = findExchanger(id);
        requireVersion(update.version(), exchanger.getVersion());
        exchanger.setStatus(update.status());
        return mapper.detail(heatExchangerRepository.saveAndFlush(exchanger));
    }

    public void archive(Long id, long version) {
        HeatExchanger exchanger = findExchanger(id);
        requireVersion(version, exchanger.getVersion());
        exchanger.setStatus(CatalogStatus.ARCHIVED);
        heatExchangerRepository.saveAndFlush(exchanger);
    }

    @Transactional(readOnly = true)
    public List<ManufacturerView> manufacturers() {
        return manufacturerRepository.findAllByOrderByNameAsc().stream().map(mapper::manufacturer).toList();
    }

    @Transactional(readOnly = true)
    public ManufacturerView manufacturer(Long id) {
        return mapper.manufacturer(findManufacturer(id));
    }

    public ManufacturerView createManufacturer(ManufacturerInput input) {
        if (manufacturerRepository.existsByNameIgnoreCase(input.name().trim())) {
            throw new CatalogConflictException("Производитель '" + input.name() + "' уже существует");
        }
        Manufacturer manufacturer = new Manufacturer();
        apply(manufacturer, input);
        return mapper.manufacturer(manufacturerRepository.saveAndFlush(manufacturer));
    }

    public ManufacturerView updateManufacturer(Long id, ManufacturerInput input) {
        Manufacturer manufacturer = findManufacturer(id);
        requireVersion(input.version(), manufacturer.getVersion());
        manufacturerRepository.findByNameIgnoreCase(input.name().trim())
                .filter(value -> !value.getId().equals(id))
                .ifPresent(value -> {
                    throw new CatalogConflictException("Производитель '" + input.name() + "' уже существует");
                });
        apply(manufacturer, input);
        return mapper.manufacturer(manufacturerRepository.saveAndFlush(manufacturer));
    }

    public void deleteManufacturer(Long id, long version) {
        Manufacturer manufacturer = findManufacturer(id);
        requireVersion(version, manufacturer.getVersion());
        if (heatExchangerRepository.countByManufacturerId(id) > 0) {
            throw new CatalogConflictException("Нельзя удалить производителя, пока на него ссылаются записи каталога");
        }
        manufacturerRepository.delete(manufacturer);
    }

    private void apply(HeatExchanger target, CatalogRecordInput input) {
        validateRanges(input);
        target.setManufacturer(findManufacturer(input.manufacturerId()));
        target.setSlug(input.slug().trim().toLowerCase(Locale.ROOT));
        target.setFamily(input.family());
        target.setModel(input.model().trim());
        target.setSeriesName(blankToNull(input.seriesName()));
        target.setGranularity(input.granularity());
        target.setStatus(input.status());
        target.setSummary(blankToNull(input.summary()));
        target.setSurfaceAreaM2(input.surfaceAreaM2());
        target.setFlowMinM3h(input.flowMinM3h());
        target.setFlowMaxM3h(input.flowMaxM3h());
        target.setPowerMinKw(null);
        target.setPowerMaxKw(null);
        target.setTemperatureMinC(input.temperatureMinC());
        target.setTemperatureMaxC(input.temperatureMaxC());
        target.setPressureMinBar(null);
        target.setPressureMaxBar(input.pressureMaxBar());
        target.setWidthMm(input.widthMm());
        target.setHeightMm(input.heightMm());
        target.setDepthMm(input.depthMm());
        target.setMassKg(input.massKg());

        target.setApplications(new LinkedHashSet<>(resolveApplications(input.applicationCodes())));
        target.setMaterials(new LinkedHashSet<>(resolveMaterials(input.materialCodes())));
        target.getFacts().clear();
        int index = 0;
        for (FactInput value : nullSafe(input.facts())) {
            SpecificationFact fact = new SpecificationFact();
            fact.setKey(value.key().trim());
            fact.setLabel(value.label().trim());
            fact.setValue(value.value().trim());
            fact.setUnit(blankToNull(value.unit()));
            fact.setSortOrder(index++);
            target.addFact(fact);
        }
        target.getSources().clear();
        for (SourceInput value : nullSafe(input.sources())) {
            SourceReference source = new SourceReference();
            source.setTitle(value.title().trim());
            source.setUrl(value.url().trim());
            source.setCheckedOn(value.checkedOn());
            source.setMeasurementBasis(value.measurementBasis().trim());
            target.addSource(source);
        }
        target.getPressureLimits().clear();
        for (PressureLimitInput value : nullSafe(input.pressureLimits())) {
            PressureLimit limit = new PressureLimit();
            limit.setTemperatureC(value.temperatureC());
            limit.setMaxPressureBar(value.maxPressureBar());
            limit.setNote(blankToNull(value.note()));
            target.addPressureLimit(limit);
        }
    }

    private void apply(Manufacturer target, ManufacturerInput input) {
        target.setName(input.name().trim());
        target.setCountry(blankToNull(input.country()));
        target.setWebsiteUrl(blankToNull(input.websiteUrl()));
    }

    private List<ApplicationArea> resolveApplications(Set<String> inputCodes) {
        Set<String> codes = normalizeCodes(inputCodes);
        if (codes.isEmpty()) {
            return List.of();
        }
        List<ApplicationArea> found = applicationAreaRepository.findByCodeIn(codes);
        Set<String> foundCodes = found.stream().map(ApplicationArea::getCode).collect(Collectors.toSet());
        Set<String> missing = new HashSet<>(codes);
        missing.removeAll(foundCodes);
        if (!missing.isEmpty()) {
            throw new CatalogBadRequestException("Неизвестные области применения", missing.stream().sorted().toList());
        }
        return found;
    }

    private List<ConstructionMaterial> resolveMaterials(Set<String> inputCodes) {
        Set<String> codes = normalizeCodes(inputCodes);
        if (codes.isEmpty()) {
            return List.of();
        }
        List<ConstructionMaterial> found = materialRepository.findByCodeIn(codes);
        Set<String> foundCodes = found.stream().map(ConstructionMaterial::getCode).collect(Collectors.toSet());
        Set<String> missing = new HashSet<>(codes);
        missing.removeAll(foundCodes);
        if (!missing.isEmpty()) {
            throw new CatalogBadRequestException("Неизвестные материалы", missing.stream().sorted().toList());
        }
        return found;
    }

    private void validateRanges(CatalogRecordInput input) {
        List<String> errors = new java.util.ArrayList<>();
        validateRange("flowMinM3h/flowMaxM3h", input.flowMinM3h(), input.flowMaxM3h(), errors);
        validateRange("temperatureMinC/temperatureMaxC", input.temperatureMinC(), input.temperatureMaxC(), errors);
        if (!errors.isEmpty()) {
            throw new CatalogBadRequestException("Некорректные диапазоны характеристик", errors);
        }
    }

    private static void validateRange(String name, BigDecimal min, BigDecimal max, List<String> errors) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            errors.add(name + ": минимум не может быть больше максимума");
        }
    }

    private HeatExchanger findExchanger(Long id) {
        return heatExchangerRepository.findOneById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Теплообменник с id=" + id + " не найден"));
    }

    private Manufacturer findManufacturer(Long id) {
        return manufacturerRepository.findById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Производитель с id=" + id + " не найден"));
    }

    private static void requireVersion(Long supplied, long actual) {
        if (supplied == null) {
            throw new CatalogBadRequestException("Для изменения необходимо передать version");
        }
        if (supplied != actual) {
            throw new CatalogConflictException("Запись уже изменена другим пользователем; обновите данные");
        }
    }

    private static Set<String> normalizeCodes(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    private static <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
