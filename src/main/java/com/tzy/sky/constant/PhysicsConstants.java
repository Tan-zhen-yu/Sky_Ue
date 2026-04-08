package com.tzy.sky.constant;

public final class PhysicsConstants {

    private PhysicsConstants() {}

    public static final double TIME_SCALE_EPSILON = 0.0001;

    public static final double MIN_ORBIT_ALTITUDE_KM = 150.0;
    public static final double ATMOSPHERIC_SCALE_HEIGHT_KM = 50.0;
    public static final double ATMOSPHERIC_REFERENCE_ALTITUDE_KM = 400.0;
    public static final double ATMOSPHERIC_BASE_DECAY_RATE_KM_PER_S = 0.0005;

    public static final double MIN_SOLAR_EFFICIENCY = 0.1;
    public static final double MAX_SOLAR_EFFICIENCY_OFFSET_DEGREES = 90.0;

    public static final double DEEP_SPACE_TEMPERATURE_CELSIUS = -150.0;
    public static final double SOLAR_DIRECT_HEAT_CELSIUS = 230.0;
    public static final double EARTH_IR_HEAT_CELSIUS = 70.0;
    public static final double EARTH_ALBEDO_HEAT_CELSIUS = 50.0;

    public static final double BATTERY_MIN = 0.0;
    public static final double BATTERY_MAX = 100.0;

    public static final double DEFAULT_BATTERY_LEVEL = 100.0;
    public static final double DEFAULT_TEMPERATURE_CELSIUS = 20.0;

    public static final String STATE_CHARGING = "charging";
    public static final String STATE_DISCHARGING = "discharging";

    public static final double MIN_TIME_SCALE = 0.0;
    public static final double MAX_TIME_SCALE = 100.0;
    public static final double MIN_ORBIT_RADIUS_KM = 6371.0 + 150.0;
}