package com.tzy.sky.service;

import com.tzy.sky.config.properties.SatelliteSimulationProperties;
import com.tzy.sky.constant.PhysicsConstants;
import com.tzy.sky.model.SatelliteModel;
import com.tzy.sky.model.PointingMode;
import com.tzy.sky.model.ThermalMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class EnergyThermalService {

    private final SatelliteSimulationProperties config;
    private double earthRadiusKm;

    public EnergyThermalService(SatelliteSimulationProperties config) {
        this.config = config;
    }

    public void initialize(double earthRadiusKm) {
        this.earthRadiusKm = earthRadiusKm;
    }

    public double calculateSolarEfficiency(SatelliteModel sat, boolean inShadow, double currentPhase) {
        if (inShadow) return 0.0;

        if (PointingMode.SUN.equals(sat.getPointingModeValue())) {
            double offset = Math.abs(sat.getManualYaw()) + Math.abs(sat.getManualPitch());
            return Math.max(0.0, 1.0 - (offset / PhysicsConstants.MAX_SOLAR_EFFICIENCY_OFFSET_DEGREES));
        }

        double phaseRad = Math.toRadians(currentPhase);
        return Math.max(PhysicsConstants.MIN_SOLAR_EFFICIENCY, Math.abs(Math.cos(phaseRad)));
    }

    public void updateEnergyAndThermal(SatelliteModel sat, double dt, boolean inShadow,
                                        double solarEfficiency, Map<String, Double> engineTimerMap) {
        double chargeRate = (!inShadow) ? config.getPhysics().getEnergy().getSolarChargeRate() * solarEfficiency * dt : 0.0;
        double baseDrain = config.getPhysics().getEnergy().getBaseDrainRate() * dt;

        double thermalDrain = 0.0;
        ThermalMode tMode = sat.getThermalModeValue();
        if (ThermalMode.HEATER.equals(tMode) || ThermalMode.COOLER.equals(tMode)) {
            thermalDrain = config.getPhysics().getEnergy().getThermalDrainRate() * dt;
        }

        double newBattery = sat.getBattery() + chargeRate - baseDrain - thermalDrain;
        sat.setBattery(Math.max(PhysicsConstants.BATTERY_MIN, Math.min(PhysicsConstants.BATTERY_MAX, newBattery)));

        updateTemperature(sat, dt, inShadow, engineTimerMap);
    }

    private void updateTemperature(SatelliteModel sat, double dt, boolean inShadow,
                                   Map<String, Double> engineTimerMap) {
        double distanceRatio = earthRadiusKm / sat.getOrbitRadius();
        double viewFactor = distanceRatio * distanceRatio;

        double targetTemp = PhysicsConstants.DEEP_SPACE_TEMPERATURE_CELSIUS;
        targetTemp += PhysicsConstants.EARTH_IR_HEAT_CELSIUS * viewFactor;

        if (!inShadow) {
            targetTemp += PhysicsConstants.SOLAR_DIRECT_HEAT_CELSIUS;
            targetTemp += PhysicsConstants.EARTH_ALBEDO_HEAT_CELSIUS * viewFactor;
        }

        Double activeTime = engineTimerMap.get(sat.getId());
        if (activeTime != null) {
            targetTemp += (activeTime > config.getPhysics().getPropulsion().getEngineWarmupTime()) ?
                    config.getPhysics().getThermal().getEngineActiveHeat() : config.getPhysics().getThermal().getEngineWarmupHeat();
        }

        ThermalMode tMode = sat.getThermalModeValue();
        if (ThermalMode.HEATER.equals(tMode)) targetTemp = config.getPhysics().getThermal().getHeaterTarget();
        else if (ThermalMode.COOLER.equals(tMode)) targetTemp = config.getPhysics().getThermal().getCoolerTarget();

        double tempChange = (targetTemp - sat.getTemperature()) * config.getPhysics().getThermal().getTemperatureChangeRate() * dt;
        sat.setTemperature(sat.getTemperature() + tempChange);
    }
}