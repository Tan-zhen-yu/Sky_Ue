package com.tzy.sky.model;

import com.tzy.sky.constant.PhysicsConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SatelliteModel {

    private String id;
    private double orbitRadius;
    private double speed;
    private double startPhase;
    private double inclination;

    private double manualPitch = 0.0;
    private double manualYaw = 0.0;
    private double manualRoll = 0.0;

    private double realPitch;
    private double realYaw;
    private double realRoll;

    private double battery = PhysicsConstants.DEFAULT_BATTERY_LEVEL;
    private boolean isOccluded = false;

    private double temperature = PhysicsConstants.DEFAULT_TEMPERATURE_CELSIUS;
    private ThermalMode thermalMode = ThermalMode.AUTO;
    private PointingMode pointingMode = PointingMode.EARTH;
    private boolean isScanning;

    private String connectedStation;
    private double solarEfficiency;
    private boolean engineActive;

    public SatelliteModel(String id, double orbitRadius, double startPhase, double inclination) {
        this.id = id;
        this.orbitRadius = orbitRadius;
        this.startPhase = startPhase;
        this.inclination = inclination;
        this.battery = PhysicsConstants.DEFAULT_BATTERY_LEVEL;
        this.temperature = PhysicsConstants.DEFAULT_TEMPERATURE_CELSIUS;
        this.thermalMode = ThermalMode.AUTO;
        this.pointingMode = PointingMode.EARTH;
    }

    public ThermalMode getThermalModeValue() {
        return thermalMode;
    }

    public String getThermalMode() {
        return thermalMode.getValue();
    }

    public PointingMode getPointingModeValue() {
        return pointingMode;
    }

    public String getPointingMode() {
        return pointingMode.getValue();
    }
}