package com.axel20378.heat_exchanger_selector.catalog.controller;

import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.ManufacturerInput;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.ManufacturerView;
import com.axel20378.heat_exchanger_selector.catalog.service.CatalogAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/manufacturers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManufacturerController {
    private final CatalogAdminService service;

    public AdminManufacturerController(CatalogAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<ManufacturerView> list() {
        return service.manufacturers();
    }

    @GetMapping("/{id}")
    public ManufacturerView get(@PathVariable Long id) {
        return service.manufacturer(id);
    }

    @PostMapping
    public ResponseEntity<ManufacturerView> create(@Valid @RequestBody ManufacturerInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createManufacturer(input));
    }

    @PutMapping("/{id}")
    public ManufacturerView update(@PathVariable Long id, @Valid @RequestBody ManufacturerInput input) {
        return service.updateManufacturer(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam long version) {
        service.deleteManufacturer(id, version);
        return ResponseEntity.noContent().build();
    }
}
