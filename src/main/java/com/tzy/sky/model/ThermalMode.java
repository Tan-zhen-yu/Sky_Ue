package com.tzy.sky.model;

public enum ThermalMode {
    AUTO("auto"),
    HEATER("heater"),
    COOLER("cooler");

    private final String value;

    ThermalMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ThermalMode fromString(String text) {
        if (text == null) return null;
        for (ThermalMode mode : ThermalMode.values()) {
            if (mode.value.equalsIgnoreCase(text)) {
                return mode;
            }
        }
        return null;
    }

    public static ThermalMode fromStringOrDefault(String text, ThermalMode defaultMode) {
        ThermalMode mode = fromString(text);
        return mode != null ? mode : defaultMode;
    }
}