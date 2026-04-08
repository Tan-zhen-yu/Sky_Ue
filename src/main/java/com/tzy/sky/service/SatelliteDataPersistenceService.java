// service/SatelliteDataPersistenceService.java
package com.tzy.sky.service;

import com.tzy.sky.entity.SatelliteData;
import com.tzy.sky.entity.SatelliteDataSampled;
import com.tzy.sky.mapper.SatelliteDataMapper;
import com.tzy.sky.mapper.SatelliteDataSampledMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SatelliteDataPersistenceService {

    @Autowired
    private SatelliteDataMapper dataMapper;

    @Autowired
    private SatelliteDataSampledMapper sampledMapper;

    // 批量保存配置
    private static final int BATCH_SIZE = 100;

    // 每秒采样计数器（按卫星ID分组）
    private final ConcurrentHashMap<String, AtomicInteger> sampleCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastSecondMap = new ConcurrentHashMap<>();

    /**
     * 保存单条数据（双表存储）
     * 完整数据表：存所有数据
     * 降采样表：每秒存第一条
     */
    public void save(SatelliteData data) {
        if (data == null) return;

        // 1. 保存到完整数据表
        dataMapper.insert(data);

        // 2. 判断是否保存到降采样表（每秒第一条）
        if (shouldSaveToSampled(data.getSatelliteId(), data.getTimestamp())) {
            SatelliteDataSampled sampled = convertToSampled(data);
            sampledMapper.insert(sampled);
        }
    }

    /**
     * 批量保存（高性能版本）
     */
    @Async("taskExecutor")
    public void saveBatch(List<SatelliteData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        // 1. 批量保存完整数据
        dataMapper.insertBatch(dataList);

        // 2. 筛选需要降采样的数据（每秒第一条）
        List<SatelliteDataSampled> sampledList = dataList.stream()
                .filter(d -> shouldSaveToSampled(d.getSatelliteId(), d.getTimestamp()))
                .map(this::convertToSampled)
                .collect(Collectors.toList());

        // 3. 批量保存降采样数据（使用 XML 批量插入）
        if (!sampledList.isEmpty()) {
            sampledMapper.insertBatch(sampledList);
        }

        log.debug("批量保存完成: 完整数据{}条, 降采样{}条", dataList.size(), sampledList.size());
    }

    /**
     * 判断是否应该保存到降采样表（每秒第一条）
     */
    private boolean shouldSaveToSampled(String satelliteId, LocalDateTime timestamp) {
        if (timestamp == null) return false;

        long currentSecond = timestamp.toEpochSecond(java.time.ZoneOffset.UTC);
        Long lastSecond = lastSecondMap.get(satelliteId);

        if (lastSecond == null || currentSecond != lastSecond) {
            // 新的秒，重置计数器并保存
            lastSecondMap.put(satelliteId, currentSecond);
            sampleCounters.put(satelliteId, new AtomicInteger(1));
            return true;
        }

        return false; // 同一秒内只存第一条
    }

    /**
     * 实体转换
     */
    private SatelliteDataSampled convertToSampled(SatelliteData data) {
        return SatelliteDataSampled.builder()
                .id(data.getId())
                .satelliteId(data.getSatelliteId())
                .simulationTime(data.getSimulationTime())
                .timestamp(data.getTimestamp())
                .positionX(data.getPositionX())
                .positionY(data.getPositionY())
                .positionZ(data.getPositionZ())
                .orbitRadius(data.getOrbitRadius())
                .realPitch(data.getRealPitch())
                .realRoll(data.getRealRoll())
                .realYaw(data.getRealYaw())
                .battery(data.getBattery())
                .solarEfficiency(data.getSolarEfficiency())
                .temperature(data.getTemperature())
                .thermalMode(data.getThermalMode())
                .engineActive(data.getEngineActive())
                .isOccluded(data.getIsOccluded())
                .isScanning(data.getIsScanning())
                .pointingMode(data.getPointingMode())
                .connectedStation(data.getConnectedStation())
                .speed(data.getSpeed())
                .createdAt(data.getCreatedAt())
                .build();
    }

    /**
     * 批量保存优化版本（带缓冲队列）
     */
    public void saveWithBuffer(SatelliteData data) {
        DataBuffer.getInstance().add(data, this);
    }
}