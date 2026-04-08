package com.tzy.sky.model;

public enum AttitudeAxis {
    PITCH("pitch"),
    YAW("yaw"),
    ROLL("roll");

    private final String value;

    AttitudeAxis(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AttitudeAxis fromString(String text) {
        if (text == null) return null;
        for (AttitudeAxis axis : AttitudeAxis.values()) {
            if (axis.value.equalsIgnoreCase(text)) {
                return axis;
            }
        }
        return null;
    }
}