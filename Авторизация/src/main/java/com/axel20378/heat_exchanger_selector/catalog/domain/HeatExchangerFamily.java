package com.axel20378.heat_exchanger_selector.catalog.domain;

public enum HeatExchangerFamily {
    PLATE("Пластинчатый"),
    SHELL_AND_TUBE("Кожухотрубный"),
    AIR_COOLED("Воздушного охлаждения"),
    SPIRAL("Спиральный");

    private final String label;

    HeatExchangerFamily(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
