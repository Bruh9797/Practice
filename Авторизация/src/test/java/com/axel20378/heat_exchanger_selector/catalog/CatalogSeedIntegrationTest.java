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
    void loadsExactlyFortyTwoSourcedRecordsWithRequiredFamilyDistribution() {
        var records = repository.findAllWithDetails();

        assertThat(records).hasSize(42);
        Map<HeatExchangerFamily, Long> counts = records.stream()
                .collect(Collectors.groupingBy(value -> value.getFamily(), Collectors.counting()));
        assertThat(counts).containsEntry(HeatExchangerFamily.PLATE, 24L)
                .containsEntry(HeatExchangerFamily.SHELL_AND_TUBE, 6L)
                .containsEntry(HeatExchangerFamily.AIR_COOLED, 6L)
                .containsEntry(HeatExchangerFamily.SPIRAL, 6L);
        assertThat(records).allSatisfy(value -> {
            assertThat(value.getSources()).hasSize(1);
            assertThat(value.getSources().iterator().next().getCheckedOn()).isEqualTo(LocalDate.of(2026, 7, 12));
            assertThat(value.getSources().iterator().next().getUrl()).startsWith("https://");
            assertThat(value.getSources().iterator().next().getMeasurementBasis()).contains("[DEMO]");
            assertThat(value.getSurfaceAreaM2()).isNotNull();
            assertThat(value.getFlowMinM3h()).isNotNull();
            assertThat(value.getFlowMaxM3h()).isNotNull();
            assertThat(value.getPowerMinKw()).isNotNull();
            assertThat(value.getPowerMaxKw()).isNotNull();
            assertThat(value.getTemperatureMinC()).isNotNull();
            assertThat(value.getTemperatureMaxC()).isNotNull();
            assertThat(value.getPressureMinBar()).isNotNull();
            assertThat(value.getPressureMaxBar()).isNotNull();
            assertThat(value.getWidthMm()).isNotNull();
            assertThat(value.getHeightMm()).isNotNull();
            assertThat(value.getDepthMm()).isNotNull();
            assertThat(value.getMassKg()).isNotNull();
            assertThat(value.getPressureLimits()).isNotEmpty();
            assertThat(value.getFacts()).anySatisfy(fact -> {
                assertThat(fact.getKey()).isEqualTo("mockFields");
                assertThat(fact.getValue()).isNotBlank();
            });
        });
    }

    @Test
    void seedingIsIdempotent() throws Exception {
        seeder.run(new DefaultApplicationArguments(new String[0]));
        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertThat(repository.count()).isEqualTo(42);
    }
}
