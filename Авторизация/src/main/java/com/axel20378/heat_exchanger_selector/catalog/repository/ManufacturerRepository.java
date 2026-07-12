package com.axel20378.heat_exchanger_selector.catalog.repository;

import com.axel20378.heat_exchanger_selector.catalog.domain.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {
    Optional<Manufacturer> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Manufacturer> findAllByOrderByNameAsc();
}
