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

@Component
@Profile({"demo", "test", "postgres"})
public class CatalogDemoSeeder implements ApplicationRunner {
    private static final int EXPECTED_RECORDS = 42;
    private static final String DATA_RESOURCE = "data/demo-catalog.psv";

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
            Map.entry("powerBasis", "Основание диапазона мощности"),
            Map.entry("temperatureFact", "Температурный предел серии"),
            Map.entry("coilLength", "Длина теплообменного блока"),
            Map.entry("options", "Опции"),
            Map.entry("diameterBasis", "Основание габарита"),
            Map.entry("dataOrigin", "Происхождение данных"),
            Map.entry("mockFields", "Демонстрационные поля"),
            Map.entry("mockMethod", "Метод заполнения mock-данных")
    );

    private static final Map<String, ManufacturerSeed> MANUFACTURERS = Map.of(
            "Alfa Laval", new ManufacturerSeed("Швеция", "https://www.alfalaval.com"),
            "Danfoss", new ManufacturerSeed("Дания", "https://www.danfoss.com"),
            "Kelvion", new ManufacturerSeed("Германия", "https://www.kelvion.com"),
            "SWEP", new ManufacturerSeed("Швеция", "https://www.swepgroup.com"),
            "Basco", new ManufacturerSeed("США", "https://www.apiheattransfer.com")
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
                if (!heatExchangerRepository.existsBySlug(values[4])) {
                    heatExchangerRepository.save(toEntity(values, manufacturers, applications, materials));
                }
            }
        }
        if (parsed != EXPECTED_RECORDS) {
            throw new IllegalStateException("В demo-каталоге должно быть ровно " + EXPECTED_RECORDS
                    + " записей, найдено " + parsed);
        }
    }

    private HeatExchanger toEntity(String[] value,
                                   Map<String, Manufacturer> manufacturers,
                                   Map<String, ApplicationArea> applications,
                                   Map<String, ConstructionMaterial> materials) {
        HeatExchanger target = new HeatExchanger();
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
        target.setPowerMinKw(decimal(value[13]));
        target.setPowerMaxKw(decimal(value[14]));
        target.setTemperatureMinC(decimal(value[15]));
        target.setTemperatureMaxC(decimal(value[16]));
        target.setPressureMinBar(decimal(value[17]));
        target.setPressureMaxBar(decimal(value[18]));
        target.setWidthMm(decimal(value[19]));
        target.setHeightMm(decimal(value[20]));
        target.setDepthMm(decimal(value[21]));
        target.setMassKg(decimal(value[22]));

        SourceReference source = new SourceReference();
        source.setUrl(value[23]);
        source.setTitle(value[24]);
        source.setCheckedOn(LocalDate.parse(value[25]));
        source.setMeasurementBasis(value[26]);
        target.addSource(source);
        addFacts(target, value[27]);
        addPressureLimits(target, value[28], value[27]);
        return target;
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
            limit.setNote(isMockField(facts, "pressureCurve")
                    ? "Демонстрационная точка для проверки интерфейса; не использовать для расчёта"
                    : "Допустимое давление при указанной температуре по официальному листу модели");
            target.addPressureLimit(limit);
        }
    }

    private static boolean isMockField(String facts, String field) {
        return Arrays.stream(facts.split(";"))
                .filter(value -> value.startsWith("mockFields="))
                .map(value -> value.substring("mockFields=".length()))
                .flatMap(value -> Arrays.stream(value.split(",")))
                .anyMatch(field::equals);
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
            throw new IllegalStateException("Неизвестный " + type + " в demo-каталоге: " + key);
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
