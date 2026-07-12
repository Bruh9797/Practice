package com.axel20378.heat_exchanger_selector.catalog;

import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchangerFamily;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CompareRequest;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SearchRequest;
import com.axel20378.heat_exchanger_selector.catalog.exception.CatalogBadRequestException;
import com.axel20378.heat_exchanger_selector.catalog.service.CatalogQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogQueryServiceIntegrationTest {
    @Autowired
    private CatalogQueryService service;

    @Test
    void fullyPopulatedDemoCatalogNeverReturnsInsufficientArea() {
        SearchRequest request = new SearchRequest(null, Set.of(), Set.of(), Set.of(), Set.of(),
                new BigDecimal("5"), null, null, null, null, 0, 100);

        var result = service.search(request);

        assertThat(result.excludedUnknownCount()).isZero();
        assertThat(result.items()).isNotEmpty().allSatisfy(item ->
                assertThat(item.surfaceAreaM2()).isGreaterThanOrEqualTo(new BigDecimal("5")));
        assertThat(result.items()).allSatisfy(item -> assertThat(item.containsMockData()).isTrue());
    }

    @Test
    void activeCriteriaAreRankedAndPaginationOrderIsStable() {
        SearchRequest request = new SearchRequest("T6 BFG", Set.of(HeatExchangerFamily.PLATE), Set.of(),
                Set.of("HEATING"), Set.of("ALLOY_316"), null, null, null, null, null, 0, 3);

        var first = service.search(request);
        var repeated = service.search(request);

        assertThat(first.items()).isNotEmpty();
        assertThat(first.items()).extracting(item -> item.id())
                .containsExactlyElementsOf(repeated.items().stream().map(item -> item.id()).toList());
        assertThat(first.items()).allSatisfy(item -> {
            assertThat(item.score()).isBetween(0, 100);
            assertThat(item.reasons()).isNotEmpty();
            assertThat(item.manufacturer().name()).isEqualTo("Alfa Laval");
        });
    }

    @Test
    void comparePreservesRequestedOrderAndRejectsDuplicates() {
        var page = service.search(new SearchRequest(null, Set.of(), Set.of(), Set.of(), Set.of(),
                null, null, null, null, null, 0, 2));
        List<Long> reverse = List.of(page.items().get(1).id(), page.items().get(0).id());

        var comparison = service.compare(new CompareRequest(reverse));

        assertThat(comparison.items()).extracting(item -> item.id()).containsExactlyElementsOf(reverse);
        assertThatThrownBy(() -> service.compare(new CompareRequest(List.of(reverse.get(0), reverse.get(0)))))
                .isInstanceOf(CatalogBadRequestException.class);
    }
}
