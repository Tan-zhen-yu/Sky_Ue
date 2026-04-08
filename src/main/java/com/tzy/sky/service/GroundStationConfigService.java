// service/GroundStationConfigService.java
package com.tzy.sky.service;

import com.tzy.sky.entity.GroundStationConfig;
import com.tzy.sky.mapper.GroundStationConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 地面站配置服务类
 * 负责从数据库加载和管理地面站静态配置
 */
@Slf4j
@Service
public class GroundStationConfigService {

    @Autowired
    private GroundStationConfigMapper groundStationConfigMapper;

    /**
     * 获取所有启用的地面站配置
     * @return 地面站配置列表
     */
    public List<GroundStationConfig> getAllEnabledStations() {
        return groundStationConfigMapper.selectAllEnabled();
    }

    /**
     * 获取所有地面站配置（包括禁用的）
     * @return 所有地面站配置列表
     */
    public List<GroundStationConfig> getAllStations() {
        return groundStationConfigMapper.selectList(null);
    }

    /**
     * 根据ID获取地面站配置
     * @param id 地面站ID
     * @return 地面站配置
     */
    public GroundStationConfig getStationById(String id) {
        return groundStationConfigMapper.selectByStationId(id);
    }

    /**
     * 保存或更新地面站配置
     * @param config 地面站配置
     * @return 是否成功
     */
    public boolean saveOrUpdateStation(GroundStationConfig config) {
        GroundStationConfig existing = groundStationConfigMapper.selectById(config.getId());
        if (existing == null) {
            int result = groundStationConfigMapper.insert(config);
            log.info("新增地面站配置: {}", config.getId());
            return result > 0;
        } else {
            int result = groundStationConfigMapper.updateById(config);
            log.info("更新地面站配置: {}", config.getId());
            return result > 0;
        }
    }

    /**
     * 删除地面站配置
     * @param id 地面站ID
     * @return 是否成功
     */
    public boolean deleteStation(String id) {
        int result = groundStationConfigMapper.deleteById(id);
        log.info("删除地面站配置: {}", id);
        return result > 0;
    }

    /**
     * 检查是否有任何地面站配置
     * @return 是否有配置
     */
    public boolean hasStationConfigs() {
        Long count = groundStationConfigMapper.selectCount(null);
        return count != null && count > 0;
    }

    /**
     * 初始化时检查配置
     */
    @PostConstruct
    public void init() {
        log.info("=== GroundStationConfigService 初始化 ===");
        List<GroundStationConfig> stations = getAllEnabledStations();
        log.info("从数据库加载了 {} 个启用状态的地面站配置", stations.size());
        for (GroundStationConfig station : stations) {
            log.info("   - {}: ({}, {}, {})",
                    station.getName(), station.getX(), station.getY(), station.getZ());
        }
    }
}
