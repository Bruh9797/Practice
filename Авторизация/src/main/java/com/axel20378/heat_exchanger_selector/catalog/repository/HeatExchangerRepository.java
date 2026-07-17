package com.axel20378.heat_exchanger_selector.catalog.repository;

import com.axel20378.heat_exchanger_selector.catalog.domain.CatalogStatus;
import com.axel20378.heat_exchanger_selector.catalog.domain.HeatExchanger;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HeatExchangerRepository extends JpaRepository<HeatExchanger, Long> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {"manufacturer", "applications", "materials", "facts", "sources", "pressureLimits"})
    Optional<HeatExchanger> findOneById(Long id);

    @EntityGraph(attributePaths = {"manufacturer", "applications", "materials", "facts", "sources", "pressureLimits"})
    Optional<HeatExchanger> findBySlug(String slug);

    @EntityGraph(attributePaths = {"manufacturer", "applications", "materials", "facts", "sources", "pressureLimits"})
    Optional<HeatExchanger> findOneBySlugAndStatus(String slug, CatalogStatus status);

    @EntityGraph(attributePaths = {"manufacturer", "applications", "materials", "facts", "sources", "pressureLimits"})
    List<HeatExchanger> findAllByStatus(CatalogStatus status);

    @EntityGraph(attributePaths = {"manufacturer", "applications", "materials", "facts", "sources", "pressureLimits"})
    List<HeatExchanger> findAllByIdInAndStatus(Collection<Long> ids, CatalogStatus status);

    @EntityGraph(attributePaths = {"manufacturer", "applications", "materials", "facts", "sources", "pressureLimits"})
    @Query("select distinct h from HeatExchanger h")
    List<HeatExchanger> findAllWithDetails();

    @Query("select count(h) from HeatExchanger h where h.manufacturer.id = :manufacturerId")
    long countByManufacturerId(@Param("manufacturerId") Long manufacturerId);
}
