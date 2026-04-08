// service/SatelliteConfigService.java
package com.tzy.sky.service;

import com.tzy.sky.entity.SatelliteConfig;
import com.tzy.sky.mapper.SatelliteConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 卫星配置服务类
 * 负责从数据库加载和管理卫星静态配置
 */
@Slf4j
@Service
public class SatelliteConfigService {

    @Autowired
    private SatelliteConfigMapper satelliteConfigMapper;

    /**
     * 获取所有启用的卫星配置
     * @return 卫星配置列表
     */
    public List<SatelliteConfig> getAllEnabledSatellites() {
        return satelliteConfigMapper.selectAllEnabled();
    }

    /**
     * 获取所有卫星配置（包括禁用的）
     * @return 所有卫星配置列表
     */
    public List<SatelliteConfig> getAllSatellites() {
        return satelliteConfigMapper.selectList(null);
    }

    /**
     * 根据ID获取卫星配置
     * @param id 卫星ID
     * @return 卫星配置
     */
    public SatelliteConfig getSatelliteById(String id) {
        return satelliteConfigMapper.selectBySatelliteId(id);
    }

    /**
     * 保存或更新卫星配置
     * @param config 卫星配置
     * @return 是否成功
     */
    public boolean saveOrUpdateSatellite(SatelliteConfig config) {
        SatelliteConfig existing = satelliteConfigMapper.selectById(config.getId());
        if (existing == null) {
            int result = satelliteConfigMapper.insert(config);
            log.info("新增卫星配置: {}", config.getId());
            return result > 0;
        } else {
            int result = satelliteConfigMapper.updateById(config);
            log.info("更新卫星配置: {}", config.getId());
            return result > 0;
        }
    }

    /**
     * 删除卫星配置
     * @param id 卫星ID
     * @return 是否成功
     */
    public boolean deleteSatellite(String id) {
        int result = satelliteConfigMapper.deleteById(id);
        log.info("删除卫星配置: {}", id);
        return result > 0;
    }

    /**
     * 检查是否有任何卫星配置
     * @return 是否有配置
     */
    public boolean hasSatelliteConfigs() {
        Long count = satelliteConfigMapper.selectCount(null);
        return count != null && count > 0;
    }

    /**
     * 初始化时检查配置
     */
    @PostConstruct
    public void init() {
        log.info("=== SatelliteConfigService 初始化 ===");
        List<SatelliteConfig> satellites = getAllEnabledSatellites();
        log.info("从数据库加载了 {} 颗启用状态的卫星配置", satellites.size());
        for (SatelliteConfig sat : satellites) {
            log.info("   - {}: 轨道半径={}km, 初始相位={}°, 倾角={}°",
                    sat.getId(), sat.getOrbitRadius(), sat.getStartPhase(), sat.getInclination());
        }
    }
}
