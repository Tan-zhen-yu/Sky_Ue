package com.tzy.sky.service;

import com.tzy.sky.config.properties.SatelliteSimulationProperties;
import com.tzy.sky.constant.PhysicsConstants;
import com.tzy.sky.model.SatelliteModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class OrbitDynamicsService {

    private final SatelliteSimulationProperties config;
    private double earthRadiusKm;
    private double gm;

    public OrbitDynamicsService(SatelliteSimulationProperties config) {
        this.config = config;
    }

    public void initialize(double earthRadiusKm, double gm) {
        this.earthRadiusKm = earthRadiusKm;
        this.gm = gm;
    }

    public void updateOrbitalPosition(SatelliteModel sat, double dt) {
        double r = sat.getOrbitRadius();
        double orbitalVelocity = Math.sqrt(gm / r);
        sat.setSpeed(orbitalVelocity);

        double angleDelta = Math.toDegrees((orbitalVelocity / r) * dt);
        double currentPhase = (sat.getStartPhase() + angleDelta) % 360.0;
        sat.setStartPhase(currentPhase);
    }

    public double[] calculatePosition(double r, double phase, double inclination) {
        double theta = Math.toRadians(phase);
        double incRad = Math.toRadians(inclination);
        double xFlat = r * Math.cos(theta);
        double yFlat = r * Math.sin(theta);

        return new double[]{xFlat, yFlat * Math.cos(incRad), yFlat * Math.sin(incRad)};
    }

    public boolean calculateEarthShadow(double x, double y, double z) {
        if (x < 0) {
            double distFromXAxisSq = y * y + z * z;
            if (distFromXAxisSq < earthRadiusKm * earthRadiusKm) {
                return true;
            }
        }
        return false;
    }

    public void applyAtmosphericDrag(SatelliteModel sat, double dt) {
        double altitudeKm = sat.getOrbitRadius() - earthRadiusKm;
        if (altitudeKm < PhysicsConstants.MIN_ORBIT_ALTITUDE_KM) return;

        double dragFactor = Math.exp(-(altitudeKm - PhysicsConstants.ATMOSPHERIC_REFERENCE_ALTITUDE_KM) / PhysicsConstants.ATMOSPHERIC_SCALE_HEIGHT_KM);
        double decayAmount = PhysicsConstants.ATMOSPHERIC_BASE_DECAY_RATE_KM_PER_S * dragFactor * dt;

        sat.setOrbitRadius(sat.getOrbitRadius() - decayAmount);
    }

    public void handleEnginePhysics(SatelliteModel sat, double dt, Map<String, Double> engineTimerMap) {
        if (!sat.isEngineActive()) {
            engineTimerMap.remove(sat.getId());
            return;
        }

        double activeTime = engineTimerMap.getOrDefault(sat.getId(), 0.0) + dt;
        engineTimerMap.put(sat.getId(), activeTime);

        if (activeTime > config.getPhysics().getPropulsion().getEngineWarmupTime()) {
            double fuelCost = config.getPhysics().getPropulsion().getFuelConsumptionRate() * dt;
            if (sat.getBattery() > fuelCost) {
                sat.setBattery(sat.getBattery() - fuelCost);
                sat.setOrbitRadius(sat.getOrbitRadius() + (config.getPhysics().getPropulsion().getOrbitChangeRate() * dt));
            } else {
                sat.setEngineActive(false);
                engineTimerMap.remove(sat.getId());
                log.warn("卫星 {} 燃料耗尽，发动机熄火", sat.getId());
            }
        }
    }
}