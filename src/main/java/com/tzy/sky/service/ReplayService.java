// service/ReplayService.java
package com.tzy.sky.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tzy.sky.dto.ReplayQueryDTO;
import com.tzy.sky.dto.ReplayResponseDTO;
import com.tzy.sky.dto.StreamRequestDTO;
import com.tzy.sky.entity.SatelliteDataSampled;  // 改用降采样实体
import com.tzy.sky.mapper.SatelliteDataSampledMapper;  // 改用降采样Mapper
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReplayService {

    @Autowired
    private SatelliteDataSampledMapper sampledMapper;  // 只使用降采样表

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, SseEmitter> activeStreams = new ConcurrentHashMap<>();

    /**
     * 查询回放数据（批量）- 只从降采样表查询，性能最优
     */
    public ReplayResponseDTO queryReplayData(ReplayQueryDTO queryDTO) {
        ReplayResponseDTO response = new ReplayResponseDTO();
        response.setSatelliteId(queryDTO.getSatelliteId());

        // 构建查询条件
        QueryWrapper<SatelliteDataSampled> wrapper = new QueryWrapper<>();
        wrapper.eq("satellite_id", queryDTO.getSatelliteId())
                .between("timestamp", queryDTO.getStartTime(), queryDTO.getEndTime())
                .orderByAsc("timestamp");

        // 降采样表已经是每秒1条，直接查询
        List<SatelliteDataSampled> dataList = sampledMapper.selectList(wrapper);

        // 限制返回数量
        int limit = queryDTO.getLimit() != null ? queryDTO.getLimit() : 10000;
        if (dataList.size() > limit) {
            dataList = dataList.subList(0, limit);
        }

        // 转换为动态字段格式
        List<Map<String, Object>> resultData = dataList.stream()
                .map(data -> convertToMap(data, queryDTO.getFields()))
                .collect(Collectors.toList());

        response.setData(resultData);
        response.setReturnedCount((long) resultData.size());

        if (!dataList.isEmpty()) {
            response.setStartTimestamp(toTimestamp(dataList.get(0).getTimestamp()));
            response.setEndTimestamp(toTimestamp(dataList.get(dataList.size() - 1).getTimestamp()));
        }

        return response;
    }

    /**
     * 启动流式回放（SSE）- 从降采样表流式读取
     */
    public SseEmitter startStreamReplay(StreamRequestDTO requestDTO) {
        String streamId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);

        activeStreams.put(streamId, emitter);

        executorService.submit(() -> {
            try {
                streamData(emitter, requestDTO);
            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                activeStreams.remove(streamId);
            }
        });

        // 发送初始化消息
        try {
            Map<String, String> initData = new HashMap<>();
            initData.put("streamId", streamId);
            initData.put("status", "started");
            emitter.send(SseEmitter.event().name("init").data(initData));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 流式数据推送 - 从降采样表查询
     */
    private void streamData(SseEmitter emitter, StreamRequestDTO request) {
        // 降采样表每秒1条，batchSize可以设大一些
        int batchSize = request.getBufferSize() * 20; // 补偿降采样比例
        double currentTime = request.getStartSimulationTime();
        double speed = request.getSpeed();

        while (activeStreams.containsValue(emitter)) {
            // 从降采样表查询下一批数据
            QueryWrapper<SatelliteDataSampled> wrapper = new QueryWrapper<>();
            wrapper.eq("satellite_id", request.getSatelliteId())
                    .between("simulation_time", currentTime, currentTime + batchSize)
                    .orderByAsc("simulation_time")
                    .last("LIMIT 1000"); // 防止单次查询过多

            List<SatelliteDataSampled> batch = sampledMapper.selectList(wrapper);

            if (batch.isEmpty()) {
                try {
                    emitter.send(SseEmitter.event().name("complete").data("EOF"));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
                break;
            }

            for (SatelliteDataSampled data : batch) {
                try {
                    // 降采样数据是每秒1条，根据倍速调整发送间隔
                    // 1x速度 = 每秒发送1条（间隔1000ms）
                    // 2x速度 = 每秒发送2条（间隔500ms）
                    long sleepTime = (long) (1000 / speed);
                    Thread.sleep(sleepTime);

                    emitter.send(SseEmitter.event()
                            .name("data")
                            .data(convertToMap(data, null)));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    emitter.completeWithError(e);
                    return;
                } catch (IOException e) {
                    emitter.completeWithError(e);
                    return;
                }
            }

            currentTime = batch.get(batch.size() - 1).getSimulationTime();
        }
    }

    /**
     * 停止流式回放
     */
    public void stopStream(String streamId) {
        SseEmitter emitter = activeStreams.get(streamId);
        if (emitter != null) {
            emitter.complete();
            activeStreams.remove(streamId);
        }
    }

    /**
     * 获取可用卫星列表
     */
    public List<String> getAvailableSatellites() {
        return Arrays.asList("SAT-001", "SAT-002", "SAT-003");
    }

    /**
     * 获取数据时间范围 - 从降采样表查
     */
    public Map<String, Object> getTimeRange(String satelliteId) {
        // 使用自定义SQL查询时间范围（更高效）
        Map<String, Object> range = sampledMapper.selectTimeRange(satelliteId);

        if (range == null || range.isEmpty()) {
            range = new HashMap<>();
            range.put("startTime", null);
            range.put("endTime", null);
        }
        return range;
    }

    /**
     * 获取数据统计 - 只统计降采样表
     */
    public Map<String, Object> getStatistics(String satelliteId) {
        QueryWrapper<SatelliteDataSampled> wrapper = new QueryWrapper<>();
        wrapper.eq("satellite_id", satelliteId);

        Long count = sampledMapper.selectCount(wrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("satelliteId", satelliteId);
        stats.put("totalRecords", count);
        stats.put("approximateHours", count != null ? count / 3600.0 : 0); // 每秒1条，3600条=1小时
        stats.put("dataSource", "satellite_data_sampled (1Hz)");

        return stats;
    }

    /**
     * 将 LocalDateTime 转换为毫秒时间戳
     */
    private long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) return 0;
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 实体转Map，支持字段筛选 - 使用 SatelliteDataSampled
     */
    private Map<String, Object> convertToMap(SatelliteDataSampled data, List<String> fields) {
        Map<String, Object> map = new LinkedHashMap<>();

        // 默认字段
        map.put("id", data.getId());
        map.put("satelliteId", data.getSatelliteId());
        map.put("timestamp", toTimestamp(data.getTimestamp()));
        map.put("simulationTime", data.getSimulationTime());

        // 位置信息
        if (fields == null || fields.contains("position")) {
            map.put("positionX", data.getPositionX());
            map.put("positionY", data.getPositionY());
            map.put("positionZ", data.getPositionZ());
            map.put("orbitRadius", data.getOrbitRadius());
        }

        // 姿态信息
        if (fields == null || fields.contains("attitude")) {
            map.put("realPitch", data.getRealPitch());
            map.put("realRoll", data.getRealRoll());
            map.put("realYaw", data.getRealYaw());
        }

        // 能源信息
        if (fields == null || fields.contains("power")) {
            map.put("battery", data.getBattery());
            map.put("solarEfficiency", data.getSolarEfficiency());
        }

        // 热控信息
        if (fields == null || fields.contains("thermal")) {
            map.put("temperature", data.getTemperature());
            map.put("thermalMode", data.getThermalMode());
        }

        // 状态信息
        if (fields == null || fields.contains("status")) {
            map.put("engineActive", data.getEngineActive());
            map.put("isOccluded", data.getIsOccluded());
            map.put("isScanning", data.getIsScanning());
            map.put("pointingMode", data.getPointingMode());
            map.put("connectedStation", data.getConnectedStation());
        }

        // 速度
        if (fields == null || fields.contains("dynamics")) {
            map.put("speed", data.getSpeed());
        }

        return map;
    }
}