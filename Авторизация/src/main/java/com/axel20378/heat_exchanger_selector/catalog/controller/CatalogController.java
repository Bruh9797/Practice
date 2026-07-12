package com.axel20378.heat_exchanger_selector.catalog.controller;

import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CompareRequest;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.CompareResponse;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.HeatExchangerDetail;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.Lookups;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SearchPage;
import com.axel20378.heat_exchanger_selector.catalog.dto.CatalogDtos.SearchRequest;
import com.axel20378.heat_exchanger_selector.catalog.service.CatalogQueryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/heat-exchangers")
@PreAuthorize("isAuthenticated()")
public class CatalogController {
    private final CatalogQueryService service;

    public CatalogController(CatalogQueryService service) {
        this.service = service;
    }

    @PostMapping("/search")
    public SearchPage search(@Valid @RequestBody SearchRequest request) {
        return service.search(request);
    }

    @GetMapping("/lookups")
    public Lookups lookups() {
        return service.lookups();
    }

    @GetMapping("/{slug}")
    public HeatExchangerDetail detail(@PathVariable String slug) {
        return service.detail(slug);
    }

    @PostMapping("/compare")
    public CompareResponse compare(@Valid @RequestBody CompareRequest request) {
        return service.compare(request);
    }
}
