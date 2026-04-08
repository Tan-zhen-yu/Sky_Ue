package com.tzy.sky.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzy.sky.config.properties.SatelliteSimulationProperties;
import com.tzy.sky.dto.base.GroundStationDTO;
import com.tzy.sky.dto.base.LocationDTO;
import com.tzy.sky.dto.base.RotationDTO;
import com.tzy.sky.dto.payload.SatelliteSyncPayload;
import com.tzy.sky.dto.protocol.WsAction;
import com.tzy.sky.dto.protocol.WsMessage;
import com.tzy.sky.dto.protocol.WsTarget;
import com.tzy.sky.entity.SatelliteData;
import com.tzy.sky.entity.SatelliteConfig;
import com.tzy.sky.entity.GroundStationConfig;
import com.tzy.sky.handler.MyWebSocketHandler;
import com.tzy.sky.model.SatelliteModel;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PhysicsService {

    @Autowired
    private MyWebSocketHandler webSocketHandler;

    @Autowired
    private SatelliteSimulationProperties config;
    @Autowired
    private SatelliteDataPersistenceService persistenceService;
    @Autowired
    private SatelliteConfigService satelliteConfigService;
    @Autowired
    private GroundStationConfigService groundStationConfigService;

    // 复用 Jackson 实例
    private final ObjectMapper objectMapper = new ObjectMapper();

    // === 常量定义 (从配置文件读取) ===
    private double earthRadiusKm;
    private double gm;
    private String signallingPath;
    private String ueExePath;
    // === 数据模型 ===
    private final Map<String, SatelliteModel> satellites = new ConcurrentHashMap<>();
    private final Map<String, Double> engineTimerMap = new ConcurrentHashMap<>();
    // 地面站从数据库初始化
    private List<GroundStationDTO> groundStations;

    // === 仿真状态 ===
    private double simulationTime = 0.0;
    private volatile double timeScale = 1.0; // volatile 保证多线程可见性
    private long lastNanoTime = System.nanoTime();
    
    // === 数据保存状态 ===
    private boolean dataPersistenceEnabled = true;

    // === 进程管理 ===
    private Process signallingProcess = null;
    private Process ueProcess = null;

    @PostConstruct
    public void init() {
        log.info("=== PhysicsService 初始化开始 ===");
        log.info("正在初始化物理引擎...");
        
        try {
            log.info("验证配置对象...");
            // 验证配置是否正确加载
            if (config == null) {
                log.error("config对象为null");
                throw new RuntimeException("配置文件未正确加载");
            }
            log.info("config对象: {}", config);
            
            if (config.getUe5() == null) {
                log.error("UE5配置为null");
                throw new RuntimeException("UE5配置未找到");
            }
            log.info("UE5配置: {}", config.getUe5());
            
            if (config.getUe5().getSignalling() == null) {
                log.error("UE5信令服务器配置为null");
                throw new RuntimeException("UE5信令服务器配置未找到");
            }
            log.info("UE5信令配置: {}", config.getUe5().getSignalling());
            
            if (config.getUe5().getExecutable() == null) {
                log.error("UE5可执行文件配置为null");
                throw new RuntimeException("UE5可执行文件配置未找到");
            }
            log.info("UE5可执行配置: {}", config.getUe5().getExecutable());
            
            // 初始化配置参数
            log.info("开始初始化配置参数...");
            this.earthRadiusKm = config.getPhysics().getEarth().getRadiusKm();
            this.gm = config.getPhysics().getEarth().getGm();
            this.signallingPath = config.getUe5().getSignalling().getScriptPath();
            this.ueExePath = config.getUe5().getExecutable().getPath();
            this.timeScale = config.getPhysics().getSimulation().getTimeScale();
            
            log.info("配置参数初始化完成:");
            log.info("  earthRadiusKm: {}", earthRadiusKm);
            log.info("  gm: {}", gm);
            log.info("  signallingPath: {}", signallingPath);
            log.info("  ueExePath: {}", ueExePath);
            log.info("  timeScale: {}", timeScale);
            
            // 验证文件路径是否存在
            if (signallingPath != null && !new java.io.File(signallingPath).exists()) {
                log.warn("信令服务器脚本文件不存在: {}", signallingPath);
            }
            
            if (ueExePath != null && !new java.io.File(ueExePath).exists()) {
                log.warn("UE5可执行文件不存在: {}", ueExePath);
            }

            // 1. 初始化卫星
            initSatellites();

            // 2. 初始化地面站
            initGroundStations();

            log.info("物理引擎初始化完成，卫星数量: {}", satellites.size());
            
        } catch (Exception e) {
            log.error("物理引擎初始化失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private void initGroundStations() {
        log.info("开始初始化地面站...");

        // 从数据库加载地面站配置
        if (groundStationConfigService != null && groundStationConfigService.hasStationConfigs()) {
            log.info("从数据库加载地面站配置...");
            List<GroundStationConfig> dbConfigs = groundStationConfigService.getAllEnabledStations();

            this.groundStations = dbConfigs.stream()
                .map(gs -> new GroundStationDTO(gs.getName(), gs.getX(), gs.getY(), gs.getZ()))
                .collect(java.util.stream.Collectors.toList());

            for (GroundStationConfig dbConfig : dbConfigs) {
                log.info("   - 从数据库加载地面站 {}: ({}, {}, {})",
                    dbConfig.getName(), dbConfig.getX(), dbConfig.getY(), dbConfig.getZ());
            }
            log.info("从数据库加载了 {} 个地面站配置", groundStations.size());
        } else {
            log.error("数据库中没有地面站配置，请先执行SQL初始化数据库");
            throw new RuntimeException("数据库中没有地面站配置");
        }
    }

    private void initSatellites() {
        log.info("开始初始化卫星...");

        // 从数据库加载卫星配置
        if (satelliteConfigService != null && satelliteConfigService.hasSatelliteConfigs()) {
            log.info("从数据库加载卫星配置...");
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
        } else {
            log.error("数据库中没有卫星配置，请先执行SQL初始化数据库");
            throw new RuntimeException("数据库中没有卫星配置");
        }
    }

    // ==========================================
// (20Hz)
// ==========================================
    @Scheduled(fixedRate = 50)
    public void tick() {
        long now = System.nanoTime();
        double realDt = (now - lastNanoTime) / 1_000_000_000.0;
        lastNanoTime = now;

        // 暂停判断
        if (timeScale <= 0.0001) return;

        double logicalDt = realDt * timeScale;
        simulationTime += logicalDt;

        // 执行每颗卫星的物理结算
        for (SatelliteModel sat : satellites.values()) {
            updateSatellitePhysics(sat, logicalDt);
        }
    }

    /**
     * 单个卫星物理结算与状态广播
     */
    private void updateSatellitePhysics(SatelliteModel sat, double dt) {
        // 1. 动力学 (变轨与燃料)
        handleEnginePhysics(sat, dt);

        // 2. 环境摄动: 大气阻力导致轨道衰减 (解决高度/速度死板的问题)
        applyAtmosphericDrag(sat, dt);

        // 3. 轨道解算 (开普勒)
        double r = sat.getOrbitRadius();
        double orbitalVelocity = Math.sqrt(gm / r); // 高度下降，这里的速度会自动上升！
        sat.setSpeed(orbitalVelocity);

        double angleDelta = Math.toDegrees((orbitalVelocity / r) * dt);
        double currentPhase = (sat.getStartPhase() + angleDelta) % 360.0;
        sat.setStartPhase(currentPhase);

        // 4. 坐标计算 (ECI简化)
        double[] pos = calculatePosition(r, currentPhase, sat.getInclination());
        double x = pos[0], y = pos[1], z = pos[2];

        // 5. 几何引擎: 计算真实地球阴影遮挡 (解决充放电一模一样的问题)
        boolean inShadow = calculateEarthShadow(x, y, z);
        sat.setOccluded(inShadow);

        // 6. 姿态与能量: 计算动态太阳帆板效率 (解决永远20%的问题)
        double solarEfficiency = calculateSolarEfficiencyDynamic(sat, inShadow, currentPhase);
        sat.setSolarEfficiency(solarEfficiency);

        handleEnergyAndThermal(sat, dt, inShadow, solarEfficiency);

        // 7. 地面站连接
        checkGroundStationConnection(sat, x, y, z);

        // 8. 广播同步消息
        sendSyncMessage(sat, x, y, z);

        // 9. 保存数据到数据库
        if (dataPersistenceEnabled) {
            saveSatelliteData(sat, x, y, z);
        }
    }

    private double calculateSolarEfficiencyDynamic(SatelliteModel sat, boolean inShadow, double currentPhase) {
        if (inShadow) return 0.0; // 阴影中必定无发电

        if ("sun".equals(sat.getPointingMode())) {
            double offset = Math.abs(sat.getManualYaw()) + Math.abs(sat.getManualPitch());
            return Math.max(0.0, 1.0 - (offset / 90.0));
        }

        // 解决 20% 死值BUG：如果不是强制对日定向，效率应该随着它绕太阳转动呈现周期性波动
        // 利用相位角模拟帆板光照投影截面积的变化: max(0, cos(phase))
        // 加上基础的漫反射和底噪效率 (比如 10%)
        double phaseRad = Math.toRadians(currentPhase);
        double dynamicEfficiency = Math.max(0.1, Math.abs(Math.cos(phaseRad)));

        return dynamicEfficiency;
    }

    private void applyAtmosphericDrag(SatelliteModel sat, double dt) {
        double altitudeKm = sat.getOrbitRadius() - earthRadiusKm;
        if (altitudeKm < 150) return; // 太低就不算了，现实中准备重入大气层了

        // 极简大气阻力模型：随高度指数衰减
        // 假设 400km 为基准，大气标高(Scale Height)约为 50km
        double scaleHeight = 50.0;
        double baseDecayRate = 0.0005; // 基准轨道下降率 km/s

        // 高度越低，衰减越剧烈
        double dragFactor = Math.exp(-(altitudeKm - 400.0) / scaleHeight);
        double decayAmount = baseDecayRate * dragFactor * dt;

        // 减小轨道半径 (自然衰减)
        sat.setOrbitRadius(sat.getOrbitRadius() - decayAmount);
    }

    /**
     * 新增: 本影圆柱体碰撞检测
     */
    private boolean calculateEarthShadow(double x, double y, double z) {
        // 假设太阳光平行于 X 轴正方向照射 (简化模型)
        // 那么 X < 0 的区域就是地球背光面
        if (x < 0) {
            // 计算卫星到 X 轴的距离平方
            double distFromXAxisSq = y * y + z * z;
            // 如果距离小于地球半径的平方，说明落在地球的圆柱阴影中
            if (distFromXAxisSq < earthRadiusKm * earthRadiusKm) {
                return true;
            }
        }
        return false;
    }

    /**
     * 启用数据保存
     */
    public void enableDataPersistence() {
        this.dataPersistenceEnabled = true;
        log.info("数据保存已启用");
    }

    /**
     * 禁用数据保存
     */
    public void disableDataPersistence() {
        this.dataPersistenceEnabled = false;
        log.info("数据保存已禁用");
    }

    private void handleEnginePhysics(SatelliteModel sat, double dt) {
        if (!sat.isEngineActive()) {
            engineTimerMap.remove(sat.getId());
            return;
        }

        double activeTime = engineTimerMap.getOrDefault(sat.getId(), 0.0) + dt;
        engineTimerMap.put(sat.getId(), activeTime);

        // 3秒预热后产生推力
        if (activeTime > config.getPhysics().getPropulsion().getEngineWarmupTime()) {
            double fuelCost = config.getPhysics().getPropulsion().getFuelConsumptionRate() * dt;
            if (sat.getBattery() > fuelCost) {
                sat.setBattery(sat.getBattery() - fuelCost);
                sat.setOrbitRadius(sat.getOrbitRadius() + (config.getPhysics().getPropulsion().getOrbitChangeRate() * dt)); // 升轨
            } else {
                sat.setEngineActive(false);
                engineTimerMap.remove(sat.getId());
                log.warn("卫星 {} 燃料耗尽，发动机熄火", sat.getId());
            }
        }
    }

    private double[] calculatePosition(double r, double phase, double inclination) {
        double theta = Math.toRadians(phase);
        double incRad = Math.toRadians(inclination);
        double xFlat = r * Math.cos(theta);
        double yFlat = r * Math.sin(theta);

        return new double[]{xFlat, yFlat * Math.cos(incRad), yFlat * Math.sin(incRad)};
    }

    private double calculateSolarEfficiency(SatelliteModel sat, boolean inShadow) {
        if (inShadow) return 0.0;
        if ("sun".equals(sat.getPointingMode())) {
            double offset = Math.abs(sat.getManualYaw()) + Math.abs(sat.getManualPitch());
            return Math.max(0.0, 1.0 - (offset / 90.0));
        }
        return 0.2; // 默认效率
    }

    private void handleEnergyAndThermal(SatelliteModel sat, double dt, boolean inShadow, double solarEfficiency) {
        // --- 1. 电量计算 (保持原样) ---
        double chargeRate = (!inShadow) ? config.getPhysics().getEnergy().getSolarChargeRate() * solarEfficiency * dt : 0.0;
        double baseDrain = config.getPhysics().getEnergy().getBaseDrainRate() * dt;

        double thermalDrain = 0.0;
        String tMode = sat.getThermalMode();
        if ("heater".equals(tMode) || "cooler".equals(tMode)) {
            thermalDrain = config.getPhysics().getEnergy().getThermalDrainRate() * dt;
        }

        double newBattery = sat.getBattery() + chargeRate - baseDrain - thermalDrain;
        sat.setBattery(Math.max(0.0, Math.min(100.0, newBattery)));

        // --- 2. 空间环境热力学计算 (全新升级) ---

        // 核心物理量：地球视角系数 (View Factor) 简化版，与距离平方成反比
        // 在地表附近为 1.0，随着高度增加迅速衰减
        double distanceRatio = earthRadiusKm / sat.getOrbitRadius();
        double viewFactor = distanceRatio * distanceRatio;

        // 定义基础环境温度常数 (这里你可以把它们抽离到配置文件中)
        double deepSpaceTemp = -150.0;    // 深空背景基础极寒温度 (仅供参考)
        double solarDirectHeat = 230.0;   // 太阳直射带来的升温幅度 (非遮挡时)
        double earthIrHeat = 70.0;        // 地球红外辐射带来的升温幅度 (全天候存在，受高度影响)
        double earthAlbedoHeat = 50.0;    // 地球反照率带来的升温幅度 (仅向阳面存在，受高度影响)

        // 计算当前环境的目标平衡温度
        double targetTemp = deepSpaceTemp;

        // 任何时候都会受到地球自身红外辐射的“烘烤”，离地越近越暖和
        targetTemp += earthIrHeat * viewFactor;

        if (!inShadow) {
            // 向阳面：受到太阳直射 + 地球反射光
            targetTemp += solarDirectHeat;
            targetTemp += earthAlbedoHeat * viewFactor;
        }

        // --- 3. 卫星自身设备发热 ---
        Double activeTime = engineTimerMap.get(sat.getId());
        if (activeTime != null) {
            targetTemp += (activeTime > config.getPhysics().getPropulsion().getEngineWarmupTime()) ?
                    config.getPhysics().getThermal().getEngineActiveHeat() : config.getPhysics().getThermal().getEngineWarmupHeat();
        }

        // 主动热控干预
        if ("heater".equals(tMode)) targetTemp = config.getPhysics().getThermal().getHeaterTarget();
        else if ("cooler".equals(tMode)) targetTemp = config.getPhysics().getThermal().getCoolerTarget();

        // --- 4. 牛顿冷却定律求解 ---
        // 质量越大的卫星，温度变化越慢。为了简化，我们可以认为它们的热容一样，但温差决定了变化率
        double tempChange = (targetTemp - sat.getTemperature()) * config.getPhysics().getThermal().getTemperatureChangeRate() * dt;
        sat.setTemperature(sat.getTemperature() + tempChange);
    }

    private void checkGroundStationConnection(SatelliteModel sat, double x, double y, double z) {
        double threshold = (sat.getOrbitRadius() > config.getPhysics().getConnection().getLowOrbitThreshold()) ? 
            config.getPhysics().getConnection().getHighOrbitDistance() : config.getPhysics().getConnection().getLowOrbitDistance();
        String connected = null;

        for (GroundStationDTO gs : groundStations) {
            double distSq = Math.pow(x - gs.getX(), 2) + Math.pow(y - gs.getY(), 2) + Math.pow(z - gs.getZ(), 2);
            if (distSq < threshold * threshold) { // 比较平方，减少开方运算
                connected = gs.getName();
                break;
            }
        }
        sat.setConnectedStation(connected);
    }

// ==========================================
// 消息发送辅助方法 (核心优化点)
// ==========================================

    /**
     * 通用的 WebSocket 广播方法，统一处理序列化和异常
     */
    private void broadcastMessage(WsTarget target, WsAction action, Object payload) {
        try {
            WsMessage<?> msg = WsMessage.builder().target(target).action(action).payload(payload).build();
            webSocketHandler.broadcast(objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            log.error("JSON Serialization Error for Action: {}", action, e);
        }
    }

    private void sendSyncMessage(SatelliteModel sat, double x, double y, double z) {
        SatelliteSyncPayload payload = SatelliteSyncPayload.builder().id(sat.getId()).loc(new LocationDTO(x, y, z)).rot(new RotationDTO(sat.getManualPitch(), sat.getManualRoll(), sat.getManualYaw())).batteryLevel(sat.getBattery()).temperature(sat.getTemperature()).solarEfficiency(sat.getSolarEfficiency()).state(sat.isOccluded() ? "discharging" : "charging").thermalMode(sat.getThermalMode()).pointingMode(sat.getPointingMode()).isScanning(sat.isScanning()).connectedStation(sat.getConnectedStation()).engineActive(sat.isEngineActive()).speed(sat.getSpeed()).build();

        broadcastMessage(WsTarget.SAT, WsAction.SYNC, payload);
    }

// ==========================================
// 控制 API
// ==========================================

    public void setThermalMode(String id, String mode) {
        SatelliteModel sat = findSatellite(id);
        if (sat != null && !sat.getThermalMode().equals(mode)) {
            sat.setThermalMode(mode);
            log.info("卫星 {} 热控模式切换为 {}", sat.getId(), mode);
            broadcastStateChange(sat.getId(), "thermal", mode);
        }
    }

    public void setPointingMode(String id, String mode) {
        SatelliteModel sat = findSatellite(id);
        if (sat != null && !sat.getPointingMode().equalsIgnoreCase(mode)) {
            // 无扰切换逻辑
            if ("free".equalsIgnoreCase(mode)) {
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
            sat.setPointingMode(mode);
            broadcastStateChange(sat.getId(), "mode", mode);
        }
    }

    public void updateSensorStatus(String id, boolean isOccluded) {
        SatelliteModel sat = satellites.get(id); // 直接查表
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
        if (sat != null) sat.setOrbitRadius(radius);
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
        SatelliteModel sat = satellites.get(id);
        if (sat != null) {
            switch (axis) {
                case "pitch":
                    sat.setManualPitch(value);
                    break;
                case "yaw":
                    sat.setManualYaw(value);
                    break;
                case "roll":
                    sat.setManualRoll(value);
                    break;
                default:
                    log.warn("未知的旋转轴: {}", axis);
            }
        }
    }

    public void setTimeScale(double scale) {
        this.timeScale = scale;
        log.info("⏳ 仿真时间流速已调整为: x{}", scale);

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

        // 异步关闭视觉效果
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
                sat.setScanning(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        return "拍照指令已下发: " + photoId;
    }

    public String setFocusTarget(String id) {
        if (!satellites.containsKey(id)) return "卫星不存在";
        log.info("设置聚焦目标为卫星 [{}]", id);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", id);
        payload.put("target", id);
        broadcastMessage(WsTarget.SAT, WsAction.FOCUS_MODE, payload);

        return "Focus set to " + id;
    }

    public void viewEarth() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("target", "earth");
        broadcastMessage(WsTarget.SAT, WsAction.FOCUS_MODE, payload);
    }

// ==========================================
// 外部进程管理 (UE)
// ==========================================

    public synchronized String startUeSystem() {
        log.info("=== UE系统启动请求 ===");
        log.info("signallingPath: {}", signallingPath);
        log.info("ueExePath: {}", ueExePath);
        log.info("config: {}", config);
        
        if (ueProcess != null && ueProcess.isAlive()) {
            return "{\"status\": \"running\", \"url\": \"http://127.0.0.1:80/minimal.html\"}";
        }

        // 验证配置路径
        if (signallingPath == null || signallingPath.trim().isEmpty()) {
            log.error("信令服务器脚本路径未配置或为null");
            log.error("config.getUe5(): {}", config.getUe5());
            log.error("config.getUe5().getSignalling(): {}", config.getUe5() != null ? config.getUe5().getSignalling() : "null");
            return "{\"status\": \"error\", \"message\": \"信令服务器脚本路径未配置\"}";
        }
        
        if (ueExePath == null || ueExePath.trim().isEmpty()) {
            log.error("UE5可执行文件路径未配置或为null");
            return "{\"status\": \"error\", \"message\": \"UE5可执行文件路径未配置\"}";
        }

        new Thread(() -> {
            try {
                log.info("启动信令服务器...");
                log.info("脚本路径: {}", signallingPath);
                
                // 验证脚本文件是否存在
                File scriptFile = new File(signallingPath);
                if (!scriptFile.exists()) {
                    log.error("信令服务器脚本文件不存在: {}", signallingPath);
                    return;
                }
                
                ProcessBuilder pbSig = new ProcessBuilder("cmd.exe", "/c", signallingPath);
                pbSig.inheritIO();
                this.signallingProcess = pbSig.start();

                log.info("等待信令服务器启动...");
                TimeUnit.SECONDS.sleep(config.getUe5().getSignalling().getStartupDelay());

                log.info("启动 UE 实例...");
                log.info("UE5路径: {}", ueExePath);
                
                // 验证UE5可执行文件是否存在
                File ueFile = new File(ueExePath);
                if (!ueFile.exists()) {
                    log.error("UE5可执行文件不存在: {}", ueExePath);
                    return;
                }
                
                // 构建UE5启动参数
                List<String> args = new ArrayList<>(Arrays.asList(
                    ueExePath,
                    "-PixelStreamingURL=" + config.getUe5().getExecutable().getPixelStreamingUrl(),
                    "-ForceRes",
                    "-ResX=" + config.getUe5().getExecutable().getResolution().getWidth(),
                    "-ResY=" + config.getUe5().getExecutable().getResolution().getHeight(),
                    config.getUe5().getExecutable().getWindow().getMode(),
                    config.getUe5().getExecutable().getWindow().getRenderOffScreen(),
                    "-ccmd=\"r.ScreenPercentage " + config.getUe5().getExecutable().getPerformance().getScreenPercentage() + "\"",
                    "-PixelStreamingEncoderRateControl=" + config.getUe5().getExecutable().getPerformance().getEncoderRateControl(),
                    "-PixelStreamingEncoderBitrate=" + config.getUe5().getExecutable().getPerformance().getEncoderBitrate(),
                    "-PixelStreamingEncoderMaxQP=" + config.getUe5().getExecutable().getPerformance().getEncoderMaxQp(),
                    "-WinY=" + config.getUe5().getExecutable().getWindow().getWinY(),
                    config.getUe5().getExecutable().getWindow().getAllowSecondaryDisplays(),
                    config.getUe5().getExecutable().getAudio()
                ));

                log.info("UE5启动参数: {}", String.join(" ", args));

                ProcessBuilder pbUE = new ProcessBuilder(args);
                pbUE.inheritIO();
                this.ueProcess = pbUE.start();

                log.info("UE 实例启动成功");
                
            } catch (Exception e) {
                log.error("UE 系统启动异常", e);
                // 清理可能已启动的进程
                if (signallingProcess != null) {
                    signallingProcess.destroyForcibly();
                    signallingProcess = null;
                }
            }
        }).start();

        return "{\"status\": \"success\", \"url\": \"http://127.0.0.1:80/minimal.html\"}";
    }

    public synchronized String stopUeSystem() {
        boolean killed = false;
        if (ueProcess != null) {
            ueProcess.destroyForcibly();
            ueProcess = null;
            killed = true;
        }
        if (signallingProcess != null) {
            signallingProcess.destroyForcibly();
            signallingProcess = null;
            killed = true;
        }
        try {
            // 补充清理 Node 进程
            Runtime.getRuntime().exec("taskkill /F /IM node.exe");
        } catch (IOException ignored) {
        }

        log.info("UE 外部进程清理完成");
        return killed ? "已尝试关闭进程" : "没有运行中的进程";
    }

    @PreDestroy
    public void cleanup() {
        stopUeSystem();
    }

// ==========================================
// 内部工具
// ==========================================

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
                    .thermalMode(sat.getThermalMode())
                    .engineActive(sat.isEngineActive())
                    .isOccluded(sat.isOccluded())
                    .isScanning(sat.isScanning())
                    .pointingMode(sat.getPointingMode())
                    .connectedStation(sat.getConnectedStation())
                    .speed(sat.getSpeed())
                    .createdAt(LocalDateTime.now())
                    .build();

            // 使用缓冲队列高性能保存
            persistenceService.saveWithBuffer(data);

        } catch (Exception e) {
            log.error("保存卫星数据失败: {}", sat.getId(), e);
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
        // 优先 Map 直取 (O(1))
        SatelliteModel sat = satellites.get(id);
        if (sat != null) return sat;

        // 容错：忽略大小写查找 (O(N))
        return satellites.values().stream().filter(s -> s.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    /**
     * 获取所有卫星的实时状态数据
     * @return 卫星状态列表
     */
    public Map<String, SatelliteModel> getAllSatellites() {
        return new HashMap<>(satellites);
    }

    /**
     * 获取指定卫星的实时状态
     * @param id 卫星ID
     * @return 卫星状态，如果不存在返回null
     */
    public SatelliteModel getSatellite(String id) {
        return findSatellite(id);
    }

    /**
     * 获取当前仿真时间
     * @return 仿真时间（秒）
     */
    public double getSimulationTime() {
        return simulationTime;
    }

    /**
     * 获取当前时间流速
     * @return 时间流速倍率
     */
    public double getTimeScale() {
        return timeScale;
    }

    public boolean isDataPersistenceEnabled() {
        return dataPersistenceEnabled;
    }
}