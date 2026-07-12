package com.axel20378.heat_exchanger_selector.catalog.controller;

import com.axel20378.heat_exchanger_selector.catalog.domain.CatalogStatus;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.AdminPage;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CatalogRecordInput;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.HeatExchangerDetail;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.StatusUpdate;
import com.axel20378.heat_exchanger_selector.catalog.service.CatalogAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/heat-exchangers")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminCatalogController {
    private final CatalogAdminService service;

    public AdminCatalogController(CatalogAdminService service) {
        this.service = service;
    }

    @GetMapping
    public AdminPage list(@RequestParam(required = false) String query,
                          @RequestParam(required = false) CatalogStatus status,
                          @RequestParam(defaultValue = "0") @Min(0) int page,
                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(query, status, page, size);
    }

    @GetMapping("/{id}")
    public HeatExchangerDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<HeatExchangerDetail> create(@Valid @RequestBody CatalogRecordInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(input));
    }

    @PutMapping("/{id}")
    public HeatExchangerDetail update(@PathVariable Long id, @Valid @RequestBody CatalogRecordInput input) {
        return service.update(id, input);
    }

    @PatchMapping("/{id}/status")
    public HeatExchangerDetail updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdate update) {
        return service.updateStatus(id, update);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id, @RequestParam long version) {
        service.archive(id, version);
        return ResponseEntity.noContent().build();
    }
}
