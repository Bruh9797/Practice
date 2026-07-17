package com.axel20378.heat_exchanger_selector.catalog.exception;

import java.util.List;

public class CatalogBadRequestException extends RuntimeException {
    private final List<String> details;

    public CatalogBadRequestException(String message) {
        this(message, List.of());
    }

    public CatalogBadRequestException(String message, List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
