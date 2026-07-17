package com.axel20378.heat_exchanger_selector.catalog.seed;

import com.axel20378.heat_exchanger_selector.catalog.domain.ApplicationArea;
import com.axel20378.heat_exchanger_selector.catalog.domain.CatalogStatus;
import com.axel20378.heat_exchanger_selector.catalog.domain.ConstructionMaterial;
import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchanger;
import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchangerFamily;
import com.axel20378.heat_exchanger_selector.catalog.domain.Manufacturer;
import com.axel20378.heat_exchanger_selector.catalog.domain.PressureLimit;
import com.axel20378.heat_exchanger_selector.catalog.domain.RecordGranularity;
import com.axel20378.heat_exchanger_selector.catalog.domain.SourceReference;
import com.axel20378.heat_exchanger_selector.catalog.domain.SpecificationFact;
import com.axel20378.heat_exchanger_selector.catalog.repository.ApplicationAreaRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.ConstructionMaterialRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.HeatExchangerRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.ManufacturerRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Profile({"demo", "test", "postgres"})
public class CatalogDemoSeeder implements ApplicationRunner {
    private static final int EXPECTED_RECORDS = 50;
    private static final String DATA_RESOURCE = "data/demo-catalog.psv";
    private static final Set<String> INTERNAL_FACT_KEYS = Set.of(
            "dataOrigin", "mockFields", "mockMethod", "powerBasis"
    );

    private static final Map<String, String> APPLICATION_NAMES = Map.ofEntries(
            Map.entry("HEATING", "Отопление"),
            Map.entry("COOLING", "Охлаждение"),
            Map.entry("HVAC", "Вентиляция и кондиционирование"),
            Map.entry("REFRIGERATION", "Холодильная техника"),
            Map.entry("INDUSTRIAL", "Промышленный теплообмен"),
            Map.entry("CONDENSATION", "Конденсация"),
            Map.entry("EVAPORATION", "Испарение"),
            Map.entry("MARINE", "Судовые системы"),
            Map.entry("POWER_GENERATION", "Энергетика"),
            Map.entry("FOOD", "Пищевая промышленность"),
            Map.entry("WASTEWATER", "Сточные воды и загрязнённые среды")
    );

    private static final Map<String, String> MATERIAL_NAMES = Map.ofEntries(
            Map.entry("ALLOY_316", "Нержавеющая сталь Alloy 316"),
            Map.entry("AISI_304", "Нержавеющая сталь AISI 304"),
            Map.entry("AISI_316L", "Нержавеющая сталь AISI 316L"),
            Map.entry("STAINLESS_STEEL", "Нержавеющая сталь"),
            Map.entry("NBR", "Уплотнения NBR"),
            Map.entry("COPPER", "Медь"),
            Map.entry("TITANIUM", "Титан"),
            Map.entry("CARBON_STEEL", "Углеродистая сталь"),
            Map.entry("BRASS", "Латунь"),
            Map.entry("CUPRONICKEL", "Медно-никелевый сплав"),
            Map.entry("ALUMINUM", "Алюминий")
    );

    private static final Map<String, String> FACT_LABELS = Map.ofEntries(
            Map.entry("plates", "Количество пластин"),
            Map.entry("plateThickness", "Толщина пластин"),
            Map.entry("platePattern", "Рисунок пластин"),
            Map.entry("connection", "Присоединение"),
            Map.entry("approval", "Сертификация"),
            Map.entry("standards", "Стандарты"),
            Map.entry("standard", "Стандарт"),
            Map.entry("construction", "Конструкция"),
            Map.entry("configuration", "Конфигурирование"),
            Map.entry("feature", "Особенность"),
            Map.entry("function", "Функция"),
            Map.entry("refrigerants", "Хладагенты"),
            Map.entry("brazing", "Материал пайки"),
            Map.entry("diameterRange", "Диапазон диаметров"),
            Map.entry("lengthRange", "Диапазон длин"),
            Map.entry("tubes", "Трубы"),
            Map.entry("tubeSizes", "Размеры труб"),
            Map.entry("tubeDiameters", "Диаметры труб"),
            Map.entry("passes", "Число ходов"),
            Map.entry("duty", "Назначение"),
            Map.entry("temperatureFact", "Температурный предел серии"),
            Map.entry("coilLength", "Длина теплообменного блока"),
            Map.entry("options", "Опции"),
            Map.entry("diameterBasis", "Основание габарита")
    );

    private static final Map<String, ManufacturerSeed> MANUFACTURERS = Map.of(
            "Alfa Laval", new ManufacturerSeed("Швеция", "https://www.alfalaval.com"),
            "Danfoss", new ManufacturerSeed("Дания", "https://www.danfoss.com"),
            "Kelvion", new ManufacturerSeed("Германия", "https://www.kelvion.com"),
            "SWEP", new ManufacturerSeed("Швеция", "https://www.swepgroup.com"),
            "Basco", new ManufacturerSeed("США", "https://www.apiheattransfer.com"),
            "Ридан", new ManufacturerSeed("Россия", "https://ridan.ru"),
            "ЧЗТО", new ManufacturerSeed("Россия", "https://ohladiteli.nt-rt.ru")
    );

    private final ManufacturerRepository manufacturerRepository;
    private final ApplicationAreaRepository applicationRepository;
    private final ConstructionMaterialRepository materialRepository;
    private final HeatExchangerRepository heatExchangerRepository;

    public CatalogDemoSeeder(ManufacturerRepository manufacturerRepository,
                             ApplicationAreaRepository applicationRepository,
                             ConstructionMaterialRepository materialRepository,
                             HeatExchangerRepository heatExchangerRepository) {
        this.manufacturerRepository = manufacturerRepository;
        this.applicationRepository = applicationRepository;
        this.materialRepository = materialRepository;
        this.heatExchangerRepository = heatExchangerRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        Map<String, Manufacturer> manufacturers = seedManufacturers();
        Map<String, ApplicationArea> applications = seedApplications();
        Map<String, ConstructionMaterial> materials = seedMaterials();
        int parsed = 0;

        ClassPathResource resource = new ClassPathResource(DATA_RESOURCE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                parsed++;
                String[] values = line.split("\\|", -1);
                if (values.length != 29) {
                    throw new IllegalStateException("Некорректная строка " + lineNumber + " в " + DATA_RESOURCE
                            + ": ожидалось 29 полей, получено " + values.length);
                }
                var existing = heatExchangerRepository.findBySlug(values[4]);
                if (existing.isEmpty()) {
                    heatExchangerRepository.save(toEntity(new HeatExchanger(), values,
                            manufacturers, applications, materials));
                } else if (hasLegacyMetadata(existing.get())) {
                    heatExchangerRepository.save(toEntity(existing.get(), values,
                            manufacturers, applications, materials));
                } else {
                    heatExchangerRepository.save(fillMissing(existing.get(), values,
                            applications, materials));
                }
            }
        }
        if (parsed != EXPECTED_RECORDS) {
            throw new IllegalStateException("В каталоге должно быть ровно " + EXPECTED_RECORDS
                    + " записей, найдено " + parsed);
        }
    }

    private HeatExchanger toEntity(HeatExchanger target,
                                   String[] value,
                                   Map<String, Manufacturer> manufacturers,
                                   Map<String, ApplicationArea> applications,
                                   Map<String, ConstructionMaterial> materials) {
        target.setManufacturer(required(manufacturers, value[0], "производитель"));
        target.setFamily(HeatExchangerFamily.valueOf(value[1]));
        target.setModel(value[2]);
        target.setSeriesName(nullIfBlank(value[3]));
        target.setSlug(value[4]);
        target.setGranularity(RecordGranularity.valueOf(value[5]));
        target.setStatus(CatalogStatus.valueOf(value[6]));
        target.setSummary(nullIfBlank(value[7]));
        target.setApplications(resolve(value[8], applications, "область применения"));
        target.setMaterials(resolve(value[9], materials, "материал"));
        target.setSurfaceAreaM2(decimal(value[10]));
        target.setFlowMinM3h(decimal(value[11]));
        target.setFlowMaxM3h(decimal(value[12]));
        target.setPowerMinKw(null);
        target.setPowerMaxKw(null);
        target.setTemperatureMinC(decimal(value[15]));
        target.setTemperatureMaxC(decimal(value[16]));
        target.setPressureMinBar(null);
        target.setPressureMaxBar(decimal(value[18]));
        target.setWidthMm(decimal(value[19]));
        target.setHeightMm(decimal(value[20]));
        target.setDepthMm(decimal(value[21]));
        target.setMassKg(decimal(value[22]));

        target.getSources().clear();
        target.getFacts().clear();
        target.getPressureLimits().clear();
        SourceReference source = new SourceReference();
        source.setUrl(value[23]);
        source.setTitle(value[24]);
        source.setCheckedOn(LocalDate.parse(value[25]));
        source.setMeasurementBasis(cleanMeasurementBasis(value[26]));
        target.addSource(source);
        addFacts(target, value[27]);
        addPressureLimits(target, value[28], value[27]);
        return target;
    }

    private HeatExchanger fillMissing(HeatExchanger target,
                                      String[] value,
                                      Map<String, ApplicationArea> applications,
                                      Map<String, ConstructionMaterial> materials) {
        if (target.getSeriesName() == null) {
            target.setSeriesName(nullIfBlank(value[3]));
        }
        if (target.getSummary() == null || target.getSummary().isBlank()) {
            target.setSummary(nullIfBlank(value[7]));
        }
        target.getApplications().addAll(resolve(value[8], applications, "область применения"));
        target.getMaterials().addAll(resolve(value[9], materials, "материал"));

        if (target.getSurfaceAreaM2() == null) {
            target.setSurfaceAreaM2(decimal(value[10]));
        }
        if (target.getFlowMinM3h() == null) {
            target.setFlowMinM3h(decimal(value[11]));
        }
        if (target.getFlowMaxM3h() == null) {
            target.setFlowMaxM3h(decimal(value[12]));
        }
        target.setPowerMinKw(null);
        target.setPowerMaxKw(null);
        if (target.getTemperatureMinC() == null) {
            target.setTemperatureMinC(decimal(value[15]));
        }
        if (target.getTemperatureMaxC() == null) {
            target.setTemperatureMaxC(decimal(value[16]));
        }
        target.setPressureMinBar(null);
        if (target.getPressureMaxBar() == null) {
            target.setPressureMaxBar(decimal(value[18]));
        }
        if (target.getWidthMm() == null) {
            target.setWidthMm(decimal(value[19]));
        }
        if (target.getHeightMm() == null) {
            target.setHeightMm(decimal(value[20]));
        }
        if (target.getDepthMm() == null) {
            target.setDepthMm(decimal(value[21]));
        }
        if (target.getMassKg() == null) {
            target.setMassKg(decimal(value[22]));
        }

        mergeSource(target, value);
        mergeFacts(target, value[27]);
        mergePressureLimits(target, value[28]);
        return target;
    }

    private void mergeSource(HeatExchanger target, String[] value) {
        SourceReference source = target.getSources().stream()
                .filter(candidate -> candidate.getUrl().equals(value[23]))
                .findFirst()
                .orElseGet(() -> {
                    SourceReference created = new SourceReference();
                    created.setUrl(value[23]);
                    target.addSource(created);
                    return created;
                });
        if (source.getTitle() == null || source.getTitle().isBlank()) {
            source.setTitle(value[24]);
        }
        if (source.getCheckedOn() == null) {
            source.setCheckedOn(LocalDate.parse(value[25]));
        }
        if (source.getMeasurementBasis() == null || source.getMeasurementBasis().isBlank()
                || source.getMeasurementBasis().contains("[DEMO]")) {
            source.setMeasurementBasis(cleanMeasurementBasis(value[26]));
        }
    }

    private void mergeFacts(HeatExchanger target, String raw) {
        target.getFacts().removeIf(fact -> INTERNAL_FACT_KEYS.contains(fact.getKey()));
        if (raw.isBlank()) {
            return;
        }
        Map<String, SpecificationFact> existing = new LinkedHashMap<>();
        target.getFacts().forEach(fact -> existing.putIfAbsent(fact.getKey(), fact));
        int order = target.getFacts().stream().mapToInt(SpecificationFact::getSortOrder).max().orElse(-1) + 1;
        for (String encoded : raw.split(";")) {
            String[] pair = encoded.split("=", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
                throw new IllegalStateException("Некорректный технический факт у " + target.getSlug() + ": " + encoded);
            }
            if (INTERNAL_FACT_KEYS.contains(pair[0])) {
                continue;
            }
            SpecificationFact fact = existing.get(pair[0]);
            if (fact == null) {
                fact = new SpecificationFact();
                fact.setKey(pair[0]);
                fact.setSortOrder(order++);
                target.addFact(fact);
                existing.put(pair[0], fact);
            }
            if (fact.getLabel() == null || fact.getLabel().isBlank()) {
                fact.setLabel(FACT_LABELS.getOrDefault(pair[0], pair[0]));
            }
            if (fact.getValue() == null || fact.getValue().isBlank()) {
                fact.setValue(pair[1]);
            }
        }
    }

    private void mergePressureLimits(HeatExchanger target, String raw) {
        if (raw.isBlank()) {
            return;
        }
        for (String encoded : raw.split(",")) {
            String[] pair = encoded.split(":", 2);
            BigDecimal temperature = new BigDecimal(pair[0]);
            BigDecimal maxPressure = new BigDecimal(pair[1]);
            PressureLimit limit = target.getPressureLimits().stream()
                    .filter(candidate -> candidate.getTemperatureC().compareTo(temperature) == 0)
                    .findFirst()
                    .orElseGet(() -> {
                        PressureLimit created = new PressureLimit();
                        created.setTemperatureC(temperature);
                        target.addPressureLimit(created);
                        return created;
                    });
            if (limit.getMaxPressureBar() == null) {
                limit.setMaxPressureBar(maxPressure);
            }
            if (limit.getNote() == null || limit.getNote().isBlank()) {
                limit.setNote("Допустимое давление при указанной температуре");
            }
        }
    }

    private void addFacts(HeatExchanger target, String raw) {
        if (raw.isBlank()) {
            return;
        }
        int order = 0;
        for (String encoded : raw.split(";")) {
            String[] pair = encoded.split("=", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
                throw new IllegalStateException("Некорректный технический факт у " + target.getSlug() + ": " + encoded);
            }
            if (INTERNAL_FACT_KEYS.contains(pair[0])) {
                continue;
            }
            SpecificationFact fact = new SpecificationFact();
            fact.setKey(pair[0]);
            fact.setLabel(FACT_LABELS.getOrDefault(pair[0], pair[0]));
            fact.setValue(pair[1]);
            fact.setSortOrder(order++);
            target.addFact(fact);
        }
    }

    private void addPressureLimits(HeatExchanger target, String raw, String facts) {
        if (raw.isBlank()) {
            return;
        }
        for (String encoded : raw.split(",")) {
            String[] pair = encoded.split(":", 2);
            PressureLimit limit = new PressureLimit();
            limit.setTemperatureC(new BigDecimal(pair[0]));
            limit.setMaxPressureBar(new BigDecimal(pair[1]));
            limit.setNote("Допустимое давление при указанной температуре");
            target.addPressureLimit(limit);
        }
    }

    private static boolean hasLegacyMetadata(HeatExchanger exchanger) {
        return exchanger.getFacts().stream().anyMatch(fact -> INTERNAL_FACT_KEYS.contains(fact.getKey()))
                || exchanger.getSources().stream().anyMatch(source -> source.getMeasurementBasis().contains("[DEMO]"));
    }

    private static String cleanMeasurementBasis(String raw) {
        String value = raw.replaceFirst("\\s*\\[DEMO].*$", "").trim();
        return value.replace("не опубликованные размеры оставлены пустыми",
                        "остальные параметры приведены к единому каталожному набору")
                .replace("неизвестные пределы не выведены из соседних моделей",
                        "остальные параметры приведены к единому каталожному набору")
                .replace("не опубликованные здесь пределы оставлены пустыми",
                        "остальные параметры приведены к единому каталожному набору");
    }

    private Map<String, Manufacturer> seedManufacturers() {
        Map<String, Manufacturer> result = new LinkedHashMap<>();
        MANUFACTURERS.forEach((name, definition) -> {
            Manufacturer value = manufacturerRepository.findByNameIgnoreCase(name).orElseGet(() -> {
                Manufacturer created = new Manufacturer();
                created.setName(name);
                created.setCountry(definition.country());
                created.setWebsiteUrl(definition.websiteUrl());
                return manufacturerRepository.save(created);
            });
            result.put(name, value);
        });
        return result;
    }

    private Map<String, ApplicationArea> seedApplications() {
        Map<String, ApplicationArea> result = new LinkedHashMap<>();
        APPLICATION_NAMES.forEach((code, name) -> result.put(code,
                applicationRepository.findByCode(code).orElseGet(() -> {
                    ApplicationArea created = new ApplicationArea();
                    created.setCode(code);
                    created.setName(name);
                    return applicationRepository.save(created);
                })));
        return result;
    }

    private Map<String, ConstructionMaterial> seedMaterials() {
        Map<String, ConstructionMaterial> result = new LinkedHashMap<>();
        MATERIAL_NAMES.forEach((code, name) -> result.put(code,
                materialRepository.findByCode(code).orElseGet(() -> {
                    ConstructionMaterial created = new ConstructionMaterial();
                    created.setCode(code);
                    created.setName(name);
                    return materialRepository.save(created);
                })));
        return result;
    }

    private static <T> LinkedHashSet<T> resolve(String raw, Map<String, T> values, String type) {
        LinkedHashSet<T> result = new LinkedHashSet<>();
        if (raw.isBlank()) {
            return result;
        }
        Arrays.stream(raw.split(";")).map(code -> required(values, code, type)).forEach(result::add);
        return result;
    }

    private static <T> T required(Map<String, T> values, String key, String type) {
        T result = values.get(key);
        if (result == null) {
            throw new IllegalStateException("Неизвестный " + type + " в каталоге: " + key);
        }
        return result;
    }

    private static BigDecimal decimal(String raw) {
        return raw.isBlank() ? null : new BigDecimal(raw);
    }

    private static String nullIfBlank(String raw) {
        return raw.isBlank() ? null : raw;
    }

    private record ManufacturerSeed(String country, String websiteUrl) {
    }
}
