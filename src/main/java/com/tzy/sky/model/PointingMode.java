package com.tzy.sky.model;

public enum PointingMode {
    SUN("sun"),
    EARTH("earth"),
    FREE("free");

    private final String value;

    PointingMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PointingMode fromString(String text) {
        if (text == null) return null;
        for (PointingMode mode : PointingMode.values()) {
            if (mode.value.equalsIgnoreCase(text)) {
                return mode;
            }
        }
        return null;
    }

    public static PointingMode fromStringOrDefault(String text, PointingMode defaultMode) {
        PointingMode mode = fromString(text);
        return mode != null ? mode : defaultMode;
    }
}