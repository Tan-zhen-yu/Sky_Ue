package com.tzy.sky.controller;

import com.tzy.sky.model.SatelliteModel;
import com.tzy.sky.service.PhysicsService;
import com.tzy.sky.service.SatelliteConfigService;
import com.tzy.sky.entity.SatelliteConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 卫星数据查询 Controller
 * 提供获取卫星实时状态和配置的接口
 */
@RestController
@RequestMapping("/api/satellite")
@CrossOrigin
public class SatelliteDataController {

    @Autowired
    private PhysicsService physicsService;

    @Autowired
    private SatelliteConfigService satelliteConfigService;

    /**
     * 获取所有卫星的实时状态
     * @return 卫星状态列表
     */
    @GetMapping("/all")
    public Map<String, Object> getAllSatellites() {
        Map<String, SatelliteModel> satellites = physicsService.getAllSatellites();

        Map<String, Object> result = new HashMap<>();
        result.put("simulationTime", physicsService.getSimulationTime());
        result.put("timeScale", physicsService.getTimeScale());
        result.put("count", satellites.size());
        result.put("satellites", satellites.values());

        return result;
    }

    /**
     * 获取指定卫星的实时状态
     * @param id 卫星ID
     * @return 卫星状态
     */
    @GetMapping("/{id}")
    public Map<String, Object> getSatellite(@PathVariable String id) {
        SatelliteModel sat = physicsService.getSatellite(id);

        Map<String, Object> result = new HashMap<>();
        if (sat == null) {
            result.put("success", false);
            result.put("message", "卫星不存在: " + id);
            return result;
        }

        result.put("success", true);
        result.put("simulationTime", physicsService.getSimulationTime());
        result.put("satellite", sat);
        return result;
    }

    /**
     * 获取所有卫星ID列表
     * @return 卫星ID列表
     */
    @GetMapping("/ids")
    public Map<String, Object> getSatelliteIds() {
        Map<String, SatelliteModel> satellites = physicsService.getAllSatellites();

        Map<String, Object> result = new HashMap<>();
        result.put("count", satellites.size());
        result.put("ids", satellites.keySet());

        return result;
    }

    /**
     * 从数据库获取所有卫星配置（静态配置）
     * @return 卫星配置列表
     */
    @GetMapping("/configs")
    public Map<String, Object> getAllSatelliteConfigs() {
        List<SatelliteConfig> configs = satelliteConfigService.getAllSatellites();

        Map<String, Object> result = new HashMap<>();
        result.put("count", configs.size());
        result.put("configs", configs);

        return result;
    }

    /**
     * 从数据库获取指定卫星配置
     * @param id 卫星ID
     * @return 卫星配置
     */
    @GetMapping("/config/{id}")
    public Map<String, Object> getSatelliteConfig(@PathVariable String id) {
        SatelliteConfig config = satelliteConfigService.getSatelliteById(id);

        Map<String, Object> result = new HashMap<>();
        if (config == null) {
            result.put("success", false);
            result.put("message", "卫星配置不存在: " + id);
            return result;
        }

        result.put("success", true);
        result.put("config", config);
        return result;
    }
}
