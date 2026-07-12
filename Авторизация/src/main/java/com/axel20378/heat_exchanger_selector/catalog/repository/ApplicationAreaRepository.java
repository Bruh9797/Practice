package com.axel20378.heat_exchanger_selector.catalog.repository;

import com.axel20378.heat_exchanger_selector.catalog.domain.ApplicationArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationAreaRepository extends JpaRepository<ApplicationArea, Long> {
    Optional<ApplicationArea> findByCode(String code);
    List<ApplicationArea> findByCodeIn(Collection<String> codes);
    List<ApplicationArea> findAllByOrderByNameAsc();
}
