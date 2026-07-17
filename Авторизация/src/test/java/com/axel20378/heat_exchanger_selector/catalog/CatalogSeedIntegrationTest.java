package com.axel20378.heat_exchanger_selector.catalog;

import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchangerFamily;
import com.axel20378.heat_exchanger_selector.catalog.repository.HeatExchangerRepository;
import com.axel20378.heat_exchanger_selector.catalog.seed.CatalogDemoSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogSeedIntegrationTest {
    @Autowired
    private HeatExchangerRepository repository;

    @Autowired
    private CatalogDemoSeeder seeder;

    @Test
    void loadsFiftySourcedRecordsIncludingRussianAndSovietModels() {
        var records = repository.findAllWithDetails();

        assertThat(records).hasSize(50);
        Map<HeatExchangerFamily, Long> counts = records.stream()
                .collect(Collectors.groupingBy(value -> value.getFamily(), Collectors.counting()));
        assertThat(counts).containsEntry(HeatExchangerFamily.PLATE, 28L)
                .containsEntry(HeatExchangerFamily.SHELL_AND_TUBE, 10L)
                .containsEntry(HeatExchangerFamily.AIR_COOLED, 6L)
                .containsEntry(HeatExchangerFamily.SPIRAL, 6L);
        assertThat(records).allSatisfy(value -> {
            assertThat(value.getSources()).hasSize(1);
            assertThat(value.getSources().iterator().next().getCheckedOn())
                    .isBeforeOrEqualTo(LocalDate.of(2026, 7, 16));
            assertThat(value.getSources().iterator().next().getUrl()).startsWith("https://");
            assertThat(value.getSources().iterator().next().getMeasurementBasis()).doesNotContain("DEMO", "mock");
            assertThat(value.getSurfaceAreaM2()).isNotNull();
            assertThat(value.getFlowMinM3h()).isNotNull();
            assertThat(value.getFlowMaxM3h()).isNotNull();
            assertThat(value.getPowerMinKw()).isNull();
            assertThat(value.getPowerMaxKw()).isNull();
            assertThat(value.getTemperatureMinC()).isNotNull();
            assertThat(value.getTemperatureMaxC()).isNotNull();
            assertThat(value.getPressureMinBar()).isNull();
            assertThat(value.getPressureMaxBar()).isNotNull();
            assertThat(value.getWidthMm()).isNotNull();
            assertThat(value.getHeightMm()).isNotNull();
            assertThat(value.getDepthMm()).isNotNull();
            assertThat(value.getMassKg()).isNotNull();
            assertThat(value.getPressureLimits()).isNotEmpty();
            assertThat(value.getFacts()).noneSatisfy(fact ->
                    assertThat(fact.getKey()).isIn("mockFields", "mockMethod", "dataOrigin", "powerBasis"));
        });
        assertThat(records).filteredOn(value -> value.getManufacturer().getName().equals("Ридан")).hasSize(4);
        assertThat(records).filteredOn(value -> value.getManufacturer().getName().equals("ЧЗТО")).hasSize(4);
    }

    @Test
    void seedingIsIdempotent() throws Exception {
        seeder.run(new DefaultApplicationArguments(new String[0]));
        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertThat(repository.count()).isEqualTo(50);
    }

    @Test
    void fillsMissingFieldsInExistingCatalogWithoutOverwritingCuratedValues() throws Exception {
        var record = repository.findBySlug("alfa-laval-t2-bfg-15-8240125735").orElseThrow();
        record.setFlowMinM3h(null);
        record.setFlowMaxM3h(null);
        record.setTemperatureMinC(null);
        record.setTemperatureMaxC(null);
        record.setPressureMaxBar(null);
        record.setWidthMm(new BigDecimal("151"));
        record.getFacts().clear();
        record.getPressureLimits().clear();
        repository.saveAndFlush(record);

        seeder.run(new DefaultApplicationArguments(new String[0]));

        var restored = repository.findBySlug("alfa-laval-t2-bfg-15-8240125735").orElseThrow();
        assertThat(restored.getFlowMinM3h()).isNotNull();
        assertThat(restored.getFlowMaxM3h()).isNotNull();
        assertThat(restored.getTemperatureMinC()).isNotNull();
        assertThat(restored.getTemperatureMaxC()).isNotNull();
        assertThat(restored.getPressureMaxBar()).isNotNull();
        assertThat(restored.getWidthMm()).isEqualByComparingTo("151");
        assertThat(restored.getFacts()).isNotEmpty();
        assertThat(restored.getPressureLimits()).isNotEmpty();
    }
}
