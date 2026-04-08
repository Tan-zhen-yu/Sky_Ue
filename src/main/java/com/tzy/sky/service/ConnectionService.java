package com.tzy.sky.service;

import com.tzy.sky.config.properties.SatelliteSimulationProperties;
import com.tzy.sky.dto.base.GroundStationDTO;
import com.tzy.sky.model.SatelliteModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ConnectionService {

    private final SatelliteSimulationProperties config;
    private List<GroundStationDTO> groundStations;

    public ConnectionService(SatelliteSimulationProperties config) {
        this.config = config;
    }

    public void setGroundStations(List<GroundStationDTO> groundStations) {
        this.groundStations = groundStations;
    }

    public void updateConnection(SatelliteModel sat, double x, double y, double z) {
        if (groundStations == null || groundStations.isEmpty()) {
            sat.setConnectedStation(null);
            return;
        }

        double threshold = (sat.getOrbitRadius() > config.getPhysics().getConnection().getLowOrbitThreshold()) ?
            config.getPhysics().getConnection().getHighOrbitDistance() : config.getPhysics().getConnection().getLowOrbitDistance();
        String connected = null;

        for (GroundStationDTO gs : groundStations) {
            double distSq = Math.pow(x - gs.getX(), 2) + Math.pow(y - gs.getY(), 2) + Math.pow(z - gs.getZ(), 2);
            if (distSq < threshold * threshold) {
                connected = gs.getName();
                break;
            }
        }
        sat.setConnectedStation(connected);
    }
}