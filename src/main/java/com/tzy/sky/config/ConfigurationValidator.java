package com.tzy.sky.config;

import com.tzy.sky.config.properties.SatelliteSimulationProperties;
import com.tzy.sky.service.SatelliteConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 配置验证器 - 验证所有配置是否正确加载
 */
@Slf4j
@Component
public class ConfigurationValidator implements CommandLineRunner {

    @Autowired
    private SatelliteSimulationProperties config;

    @Autowired
    private SatelliteConfigService satelliteConfigService;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== 配置验证开始 ===");
        
        // 验证UE5配置
        validateUe5Config();
        
        // 验证物理引擎配置
        validatePhysicsConfig();
        
        // 验证地面站配置
        validateGroundStationsConfig();
        
        // 验证卫星配置（优先检查数据库）
        validateSatellitesConfig();
        
        log.info("配置验证完成");
    }

    private void validateUe5Config() {
        log.info("验证UE5配置...");
        
        if (config.getUe5() == null) {
            log.error("UE5配置未找到");
            return;
        }
        
        if (config.getUe5().getSignalling() == null) {
            log.error("UE5信令服务器配置未找到");
            return;
        }
        
        if (config.getUe5().getExecutable() == null) {
            log.error("UE5可执行文件配置未找到");
            return;
        }
        
        log.info("UE5信令脚本路径: {}", config.getUe5().getSignalling().getScriptPath());
        log.info("UE5可执行文件路径: {}", config.getUe5().getExecutable().getPath());
        log.info("UE5像素流URL: {}", config.getUe5().getExecutable().getPixelStreamingUrl());
        log.info("UE5分辨率: {}x{}", 
            config.getUe5().getExecutable().getResolution().getWidth(),
            config.getUe5().getExecutable().getResolution().getHeight());
    }

    private void validatePhysicsConfig() {
        log.info("验证物理引擎配置...");
        
        if (config.getPhysics() == null) {
            log.error("物理引擎配置未找到");
            return;
        }
        
        // 地球参数
        if (config.getPhysics().getEarth() != null) {
            log.info("地球半径: {} km", config.getPhysics().getEarth().getRadiusKm());
            log.info("万有引力常数: {}", config.getPhysics().getEarth().getGm());
        }
        
        // 仿真参数
        if (config.getPhysics().getSimulation() != null) {
            log.info("仿真频率: {} Hz", config.getPhysics().getSimulation().getTickRateHz());
            log.info("时间缩放: x{}", config.getPhysics().getSimulation().getTimeScale());
        }
        
        // 能源系统
        if (config.getPhysics().getEnergy() != null) {
            log.info("基础耗电速率: {}", config.getPhysics().getEnergy().getBaseDrainRate());
            log.info("太阳能充电速率: {}", config.getPhysics().getEnergy().getSolarChargeRate());
            log.info("拍照电量阈值: {}", config.getPhysics().getEnergy().getPhotoBatteryThreshold());
        }
        
        // 推进系统
        if (config.getPhysics().getPropulsion() != null) {
            log.info("发动机预热时间: {} 秒", config.getPhysics().getPropulsion().getEngineWarmupTime());
            log.info("燃料消耗速率: {}", config.getPhysics().getPropulsion().getFuelConsumptionRate());
            log.info("轨道变化速率: {}", config.getPhysics().getPropulsion().getOrbitChangeRate());
        }
        
        // 热控系统
        if (config.getPhysics().getThermal() != null) {
            log.info("阴影温度: {} °C", config.getPhysics().getThermal().getShadowTemperature());
            log.info("太阳温度: {} °C", config.getPhysics().getThermal().getSunTemperature());
            log.info("温度变化速率: {}", config.getPhysics().getThermal().getTemperatureChangeRate());
        }
    }

    private void validateGroundStationsConfig() {
        log.info("验证地面站配置...");
        
        if (config.getGroundStations() == null || config.getGroundStations().isEmpty()) {
            log.warn("地面站配置为空，将使用默认配置");
            return;
        }
        
        log.info("地面站数量: {}", config.getGroundStations().size());
        for (SatelliteSimulationProperties.GroundStationConfig gs : config.getGroundStations()) {
            log.info("   - {}: ({}, {}, {})", gs.getName(), gs.getX(), gs.getY(), gs.getZ());
        }
    }

    private void validateSatellitesConfig() {
        log.info("验证卫星配置...");
        
        // 优先检查数据库配置
        if (satelliteConfigService != null && satelliteConfigService.hasSatelliteConfigs()) {
            log.info("从数据库加载卫星配置");
            List<com.tzy.sky.entity.SatelliteConfig> dbConfigs = satelliteConfigService.getAllEnabledSatellites();
            log.info("数据库中启用状态的卫星数量: {}", dbConfigs.size());
            for (com.tzy.sky.entity.SatelliteConfig sat : dbConfigs) {
                log.info("   - {}: 轨道半径={}km, 初始相位={}°, 倾角={}°", 
                    sat.getId(), sat.getOrbitRadius(), sat.getStartPhase(), sat.getInclination());
            }
            return;
        }
        
        // 如果数据库没有配置，检查配置文件
        if (config.getSatellites() == null || config.getSatellites().isEmpty()) {
            log.warn("数据库和配置文件均无卫星配置，将使用默认配置");
            return;
        }
        
        log.info("从配置文件加载卫星配置");
        log.info("配置文件中的卫星数量: {}", config.getSatellites().size());
        for (SatelliteSimulationProperties.SatelliteConfig sat : config.getSatellites()) {
            log.info("   - {}: 轨道半径={}km, 初始相位={}°, 倾角={}°", 
                sat.getId(), sat.getOrbitRadius(), sat.getStartPhase(), sat.getInclination());
        }
    }
}
