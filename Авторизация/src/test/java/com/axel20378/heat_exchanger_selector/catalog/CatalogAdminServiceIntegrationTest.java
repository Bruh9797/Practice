package com.axel20378.heat_exchanger_selector.catalog;

import com.axel20378.heat_exchanger_selector.catalog.domain.CatalogStatus;
import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchangerFamily;
import com.axel20378.heat_exchanger_selector.catalog.domain.RecordGranularity;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CatalogRecordInput;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SourceInput;
import com.axel20378.heat_exchanger_selector.catalog.exception.CatalogConflictException;
import com.axel20378.heat_exchanger_selector.catalog.repository.HeatExchangerRepository;
import com.axel20378.heat_exchanger_selector.catalog.repository.ManufacturerRepository;
import com.axel20378.heat_exchanger_selector.catalog.service.CatalogAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogAdminServiceIntegrationTest {
    @Autowired
    private CatalogAdminService service;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private HeatExchangerRepository heatExchangerRepository;

    @Test
    void createsUpdatesAndArchivesInsteadOfDeleting() {
        Long manufacturerId = manufacturerRepository.findByNameIgnoreCase("Alfa Laval").orElseThrow().getId();
        var created = service.create(input(null, manufacturerId, "integration-test-model", "Тестовая модель"));

        assertThat(created.status()).isEqualTo(CatalogStatus.DRAFT);
        assertThat(created.sources()).hasSize(1);

        var updated = service.update(created.id(),
                input(created.version(), manufacturerId, created.slug(), "Обновлённая тестовая модель"));
        assertThat(updated.model()).isEqualTo("Обновлённая тестовая модель");
        assertThat(updated.version()).isGreaterThan(created.version());

        service.archive(updated.id(), updated.version());

        assertThat(heatExchangerRepository.findOneById(updated.id())).get()
                .extracting(value -> value.getStatus()).isEqualTo(CatalogStatus.ARCHIVED);
        assertThat(heatExchangerRepository.existsById(updated.id())).isTrue();
    }

    @Test
    void rejectsStaleVersionBeforeApplyingChanges() {
        var existing = heatExchangerRepository.findAllWithDetails().get(0);

        assertThatThrownBy(() -> service.update(existing.getId(),
                input(existing.getVersion() + 1, existing.getManufacturer().getId(), existing.getSlug(), "Чужая версия")))
                .isInstanceOf(CatalogConflictException.class);
    }

    private CatalogRecordInput input(Long version, Long manufacturerId, String slug, String model) {
        return new CatalogRecordInput(
                version,
                manufacturerId,
                slug,
                HeatExchangerFamily.PLATE,
                model,
                "Test",
                RecordGranularity.EXACT_CONFIGURATION,
                CatalogStatus.DRAFT,
                "Запись для интеграционного теста",
                Set.of("HEATING"),
                Set.of("AISI_316L"),
                new BigDecimal("1.5"),
                null,
                null,
                null,
                null,
                new BigDecimal("16"),
                null,
                null,
                null,
                null,
                List.of(),
                List.of(new SourceInput("Тестовый источник", "https://example.test/source",
                        LocalDate.of(2026, 7, 12), "Тестовая конфигурация")),
                List.of()
        );
    }
}
