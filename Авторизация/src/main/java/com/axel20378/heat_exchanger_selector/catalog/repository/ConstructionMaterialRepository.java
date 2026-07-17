package com.axel20378.heat_exchanger_selector.catalog.repository;

import com.axel20378.heat_exchanger_selector.catalog.domain.ConstructionMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConstructionMaterialRepository extends JpaRepository<ConstructionMaterial, Long> {
    Optional<ConstructionMaterial> findByCode(String code);
    List<ConstructionMaterial> findByCodeIn(Collection<String> codes);
    List<ConstructionMaterial> findAllByOrderByNameAsc();
}
