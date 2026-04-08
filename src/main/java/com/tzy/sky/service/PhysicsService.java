package com.tzy.sky.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzy.sky.config.properties.SatelliteSimulationProperties;
import com.tzy.sky.dto.base.GroundStationDTO;
import com.tzy.sky.dto.base.LocationDTO;
import com.tzy.sky.dto.base.Result;
import com.tzy.sky.dto.base.RotationDTO;
import com.tzy.sky.dto.payload.SatelliteSyncPayload;
import com.tzy.sky.dto.protocol.WsAction;
import com.tzy.sky.dto.protocol.WsMessage;
import com.tzy.sky.dto.protocol.WsTarget;
import com.tzy.sky.entity.SatelliteConfig;
import com.tzy.sky.entity.SatelliteData;
import com.tzy.sky.entity.GroundStationConfig;
import com.tzy.sky.handler.MyWebSocketHandler;
import com.tzy.sky.constant.PhysicsConstants;
import com.tzy.sky.model.AttitudeAxis;
import com.tzy.sky.model.PointingMode;
import com.tzy.sky.model.SatelliteModel;
import com.tzy.sky.model.ThermalMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class PhysicsService {

    private final MyWebSocketHandler webSocketHandler;
    private final SatelliteSimulationProperties config;
    private final SatelliteDataPersistenceService persistenceService;
    private final SatelliteConfigService satelliteConfigService;
    private final GroundStationConfigService groundStationConfigService;
    private final OrbitDynamicsService orbitDynamicsService;
    private final EnergyThermalService energyThermalService;
    private final ConnectionService connectionService;
    private final UeProcessService ueProcessService;
    private final ObjectMapper objectMapper;

    private final Map<String, SatelliteModel> satellites = new ConcurrentHashMap<>();
    private final Map<String, Double> engineTimerMap = new ConcurrentHashMap<>();

    private double simulationTime = 0.0;
    private volatile double timeScale = 1.0;
    private long lastNanoTime = System.nanoTime();
    private boolean dataPersistenceEnabled = true;

    private final ExecutorService cameraExecutor = Executors.newFixedThreadPool(4);

    public PhysicsService(
            MyWebSocketHandler webSocketHandler,
            SatelliteSimulationProperties config,
            SatelliteDataPersistenceService persistenceService,
            SatelliteConfigService satelliteConfigService,
            GroundStationConfigService groundStationConfigService,
            OrbitDynamicsService orbitDynamicsService,
            EnergyThermalService energyThermalService,
            ConnectionService connectionService,
            UeProcessService ueProcessService,
            ObjectMapper objectMapper) {
        this.webSocketHandler = webSocketHandler;
        this.config = config;
        this.persistenceService = persistenceService;
        this.satelliteConfigService = satelliteConfigService;
        this.groundStationConfigService = groundStationConfigService;
        this.orbitDynamicsService = orbitDynamicsService;
        this.energyThermalService = energyThermalService;
        this.connectionService = connectionService;
        this.ueProcessService = ueProcessService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        log.info("=== PhysicsService 初始化开始 ===");

        orbitDynamicsService.initialize(
            config.getPhysics().getEarth().getRadiusKm(),
            config.getPhysics().getEarth().getGm()
        );
        energyThermalService.initialize(config.getPhysics().getEarth().getRadiusKm());
        ueProcessService.initialize(
            config.getUe5().getSignalling().getScriptPath(),
            config.getUe5().getExecutable().getPath()
        );
        timeScale = config.getPhysics().getSimulation().getTimeScale();

        initGroundStations();
        initSatellites();

        log.info("物理引擎初始化完成，卫星数量: {}", satellites.size());
    }

    private void initGroundStations() {
        if (!groundStationConfigService.hasStationConfigs()) {
            log.error("数据库中没有地面站配置，请先执行SQL初始化数据库");
            throw new RuntimeException("数据库中没有地面站配置");
        }

        List<GroundStationConfig> dbConfigs = groundStationConfigService.getAllEnabledStations();
        List<GroundStationDTO> groundStations = new ArrayList<>();

        for (GroundStationConfig dbConfig : dbConfigs) {
            groundStations.add(new GroundStationDTO(dbConfig.getName(), dbConfig.getX(), dbConfig.getY(), dbConfig.getZ()));
            log.info("   - 从数据库加载地面站 {}: ({}, {}, {})",
                dbConfig.getName(), dbConfig.getX(), dbConfig.getY(), dbConfig.getZ());
        }

        connectionService.setGroundStations(groundStations);
        log.info("从数据库加载了 {} 个地面站配置", groundStations.size());
    }

    private void initSatellites() {
        if (!satelliteConfigService.hasSatelliteConfigs()) {
            log.error("数据库中没有卫星配置，请先执行SQL初始化数据库");
            throw new RuntimeException("数据库中没有卫星配置");
        }

        List<SatelliteConfig> dbConfigs = satelliteConfigService.getAllEnabledSatellites();

        for (SatelliteConfig dbConfig : dbConfigs) {
            satellites.put(dbConfig.getId(),
                new SatelliteModel(dbConfig.getId(), dbConfig.getOrbitRadius(),
                    dbConfig.getStartPhase(), dbConfig.getInclination()));
            log.info("   - 从数据库加载卫星 {}: 轨道半径={}km, 初始相位={}°, 倾角={}°",
                dbConfig.getId(), dbConfig.getOrbitRadius(),
                dbConfig.getStartPhase(), dbConfig.getInclination());
        }
        log.info("从数据库加载了 {} 颗卫星配置", satellites.size());
    }

    @Scheduled(fixedRate = 50)
    public void tick() {
        long now = System.nanoTime();
        double realDt = (now - lastNanoTime) / 1_000_000_000.0;
        lastNanoTime = now;

        if (timeScale <= PhysicsConstants.TIME_SCALE_EPSILON) return;

        double logicalDt = realDt * timeScale;
        simulationTime += logicalDt;

        for (SatelliteModel sat : satellites.values()) {
            updateSatellitePhysics(sat, logicalDt);
        }
    }

    private void updateSatellitePhysics(SatelliteModel sat, double dt) {
        orbitDynamicsService.handleEnginePhysics(sat, dt, engineTimerMap);
        orbitDynamicsService.applyAtmosphericDrag(sat, dt);
        orbitDynamicsService.updateOrbitalPosition(sat, dt);

        double r = sat.getOrbitRadius();
        double[] pos = orbitDynamicsService.calculatePosition(r, sat.getStartPhase(), sat.getInclination());
        double x = pos[0], y = pos[1], z = pos[2];

        boolean inShadow = orbitDynamicsService.calculateEarthShadow(x, y, z);
        sat.setOccluded(inShadow);

        double solarEfficiency = energyThermalService.calculateSolarEfficiency(sat, inShadow, sat.getStartPhase());
        sat.setSolarEfficiency(solarEfficiency);
        energyThermalService.updateEnergyAndThermal(sat, dt, inShadow, solarEfficiency, engineTimerMap);

        connectionService.updateConnection(sat, x, y, z);

        sendSyncMessage(sat, x, y, z);

        if (dataPersistenceEnabled) {
            saveSatelliteData(sat, x, y, z);
        }
    }

    private void sendSyncMessage(SatelliteModel sat, double x, double y, double z) {
        SatelliteSyncPayload payload = SatelliteSyncPayload.builder()
            .id(sat.getId())
            .loc(new LocationDTO(x, y, z))
            .rot(new RotationDTO(sat.getManualPitch(), sat.getManualRoll(), sat.getManualYaw()))
            .batteryLevel(sat.getBattery())
            .temperature(sat.getTemperature())
            .solarEfficiency(sat.getSolarEfficiency())
            .state(sat.isOccluded() ? PhysicsConstants.STATE_DISCHARGING : PhysicsConstants.STATE_CHARGING)
            .thermalMode(sat.getThermalModeValue().getValue())
            .pointingMode(sat.getPointingModeValue().getValue())
            .isScanning(sat.isScanning())
            .connectedStation(sat.getConnectedStation())
            .engineActive(sat.isEngineActive())
            .speed(sat.getSpeed())
            .build();

        broadcastMessage(WsTarget.SAT, WsAction.SYNC, payload);
    }

    private void broadcastMessage(WsTarget target, WsAction action, Object payload) {
        try {
            WsMessage<?> msg = WsMessage.builder().target(target).action(action).payload(payload).build();
            webSocketHandler.broadcast(objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            log.error("JSON Serialization Error for Action: {}", action, e);
        }
    }

    private void broadcastStateChange(String id, String key, Object value) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        payload.put(key, value);
        payload.put("timestamp", System.currentTimeMillis());
        broadcastMessage(WsTarget.SAT, WsAction.STATE_CHANGE, payload);
    }

    private SatelliteModel findSatellite(String id) {
        return satellites.get(id);
    }

    private SatelliteModel findSatelliteCaseInsensitive(String id) {
        SatelliteModel sat = satellites.get(id);
        if (sat != null) return sat;
        return satellites.values().stream()
            .filter(s -> s.getId().equalsIgnoreCase(id))
            .findFirst()
            .orElse(null);
    }

    private void saveSatelliteData(SatelliteModel sat, double x, double y, double z) {
        try {
            SatelliteData data = SatelliteData.builder()
                .id(UUID.randomUUID().toString())
                .satelliteId(sat.getId())
                .simulationTime(simulationTime)
                .timestamp(LocalDateTime.now())
                .positionX(x)
                .positionY(y)
                .positionZ(z)
                .orbitRadius(sat.getOrbitRadius())
                .realPitch(sat.getRealPitch())
                .realRoll(sat.getRealRoll())
                .realYaw(sat.getRealYaw())
                .battery(sat.getBattery())
                .solarEfficiency(sat.getSolarEfficiency())
                .temperature(sat.getTemperature())
                .thermalMode(sat.getThermalModeValue().getValue())
                .engineActive(sat.isEngineActive())
                .isOccluded(sat.isOccluded())
                .isScanning(sat.isScanning())
                .pointingMode(sat.getPointingModeValue().getValue())
                .connectedStation(sat.getConnectedStation())
                .speed(sat.getSpeed())
                .createdAt(LocalDateTime.now())
                .build();

            persistenceService.saveWithBuffer(data);
        } catch (IllegalStateException e) {
            log.error("数据持久化服务状态异常，卫星 {}: {}", sat.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("保存卫星数据失败: {}", sat.getId(), e);
            throw new RuntimeException("卫星数据保存失败: " + sat.getId(), e);
        }
    }

    public void enableDataPersistence() {
        this.dataPersistenceEnabled = true;
        log.info("数据保存已启用");
    }

    public void disableDataPersistence() {
        this.dataPersistenceEnabled = false;
        log.info("数据保存已禁用");
    }

    public void setThermalMode(String id, String mode) {
        SatelliteModel sat = findSatellite(id);
        if (sat != null) {
            ThermalMode newMode = ThermalMode.fromString(mode);
            if (newMode != null && !sat.getThermalModeValue().equals(newMode)) {
                sat.setThermalMode(newMode);
                log.info("卫星 {} 热控模式切换为 {}", sat.getId(), mode);
                broadcastStateChange(sat.getId(), "thermal", mode);
            }
        }
    }

    public void setPointingMode(String id, String mode) {
        SatelliteModel sat = findSatellite(id);
        if (sat != null) {
            PointingMode newMode = PointingMode.fromString(mode);
            if (newMode != null && !sat.getPointingModeValue().equals(newMode)) {
                if (PointingMode.FREE.equals(newMode)) {
                    sat.setManualPitch(sat.getRealPitch());
                    sat.setManualYaw(sat.getRealYaw());
                    sat.setManualRoll(sat.getRealRoll());
                    log.info("卫星 {} 切换至 FREE，继承姿态", id);
                } else {
                    sat.setManualPitch(0);
                    sat.setManualYaw(0);
                    sat.setManualRoll(0);
                    log.info("卫星 {} 切换至自动模式 [{}]", id, mode);
                }
                sat.setPointingMode(newMode);
                broadcastStateChange(sat.getId(), "mode", mode);
            }
        }
    }

    public void updateSensorStatus(String id, boolean isOccluded) {
        SatelliteModel sat = satellites.get(id);
        if (sat != null) {
            sat.setOccluded(isOccluded);
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", id);
            payload.put("occluded", isOccluded);
            broadcastMessage(WsTarget.SAT, WsAction.SENSOR_REPORT, payload);
        }
    }

    public void setOrbit(String id, double radius) {
        SatelliteModel sat = satellites.get(id);
        if (sat != null) {
            if (radius < PhysicsConstants.MIN_ORBIT_RADIUS_KM) {
                log.warn("轨道半径 {} km 小于最小允许值 {} km", radius, PhysicsConstants.MIN_ORBIT_RADIUS_KM);
                radius = PhysicsConstants.MIN_ORBIT_RADIUS_KM;
            }
            sat.setOrbitRadius(radius);
            log.info("卫星 {} 轨道半径已调整为 {} km", id, radius);
        }
    }

    public void updateRealRotation(String id, double p, double y, double r) {
        SatelliteModel sat = satellites.get(id);
        if (sat != null) {
            sat.setRealPitch(p);
            sat.setRealYaw(y);
            sat.setRealRoll(r);
        }
    }

    public void setAttitude(String id, String axis, double value) {
        SatelliteModel sat = findSatellite(id);
        if (sat != null) {
            AttitudeAxis attitudeAxis = AttitudeAxis.fromString(axis);
            if (attitudeAxis != null) {
                switch (attitudeAxis) {
                    case PITCH:
                        sat.setManualPitch(value);
                        break;
                    case YAW:
                        sat.setManualYaw(value);
                        break;
                    case ROLL:
                        sat.setManualRoll(value);
                        break;
                }
                log.info("卫星 {} 姿态轴 {} 已设置为 {}", id, axis, value);
            } else {
                log.warn("未知的旋转轴: {}", axis);
            }
        }
    }

    public void setTimeScale(double scale) {
        if (scale < PhysicsConstants.MIN_TIME_SCALE || scale > PhysicsConstants.MAX_TIME_SCALE) {
            log.warn("无效的时间缩放因子: {}，有效范围: [{}, {}]", scale, PhysicsConstants.MIN_TIME_SCALE, PhysicsConstants.MAX_TIME_SCALE);
            scale = Math.max(PhysicsConstants.MIN_TIME_SCALE, Math.min(PhysicsConstants.MAX_TIME_SCALE, scale));
        }
        this.timeScale = scale;
        log.info("仿真时间流速已调整为: x{}", scale);
        Map<String, Object> payload = new HashMap<>();
        payload.put("scale", scale);
        broadcastMessage(WsTarget.SAT, WsAction.SET_TIME_SCALE, payload);
    }

    public void setSpeed(String id, double speed) {
        SatelliteModel sat = satellites.get(id);
        if (sat != null) {
            sat.setSpeed(speed);
            log.info("卫星 {} 速度调整: {}", id, speed);
        }
    }

    public void setEngineState(String id, boolean active) {
        SatelliteModel sat = satellites.get(id);
        if (sat != null) {
            sat.setEngineActive(active);
            log.info("卫星 {} 发动机状态变更为 {}", id, active ? "active" : "inactive");
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", id);
            payload.put("engineActive", active);
            broadcastMessage(WsTarget.SAT, WsAction.ENGINE_STATE, payload);
        }
    }

    public String triggerCamera(String id) {
        SatelliteModel sat = findSatellite(id);
        if (sat == null) return "卫星不存在";
        if (sat.getBattery() < config.getPhysics().getEnergy().getPhotoBatteryThreshold())
            return String.format("电量不足 (%.1f%%)，拒绝拍照", sat.getBattery());

        sat.setScanning(true);
        String photoId = "IMG-" + System.currentTimeMillis();
        log.info("卫星 [{}] 触发拍照指令", id);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        broadcastMessage(WsTarget.SAT, WsAction.TAKE_PHOTO, payload);

        cameraExecutor.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
                sat.setScanning(false);
                log.info("卫星 [{}] 拍照任务完成", id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("卫星 [{}] 拍照任务被中断", id);
            }
        });

        return "拍照指令已下发: " + photoId;
    }

    public String setFocusTarget(String id) {
        if (!satellites.containsKey(id)) return "卫星不存在";
        log.info("设置聚焦目标为卫星 [{}]", id);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        payload.put("target", id);
        broadcastMessage(WsTarget.SAT, WsAction.FOCUS_MODE, payload);

        return "已设置聚焦目标: " + id;
    }

    public void viewEarth() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("target", "earth");
        broadcastMessage(WsTarget.SAT, WsAction.FOCUS_MODE, payload);
    }

    public synchronized Result<String> startUeSystem() {
        return ueProcessService.startUeSystem();
    }

    public synchronized Result<String> stopUeSystem() {
        return ueProcessService.stopUeSystem();
    }

    @PreDestroy
    public void cleanup() {
        stopUeSystem();
        cameraExecutor.shutdown();
        try {
            if (!cameraExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cameraExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cameraExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public Map<String, SatelliteModel> getAllSatellites() {
        return Collections.unmodifiableMap(satellites);
    }

    public SatelliteModel getSatellite(String id) {
        return findSatellite(id);
    }

    public double getSimulationTime() {
        return simulationTime;
    }

    public double getTimeScale() {
        return timeScale;
    }

    public boolean isDataPersistenceEnabled() {
        return dataPersistenceEnabled;
    }
}