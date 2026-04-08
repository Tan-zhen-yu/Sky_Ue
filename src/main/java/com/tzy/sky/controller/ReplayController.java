// controller/ReplayController.java
package com.tzy.sky.controller;

import com.tzy.sky.dto.ReplayQueryDTO;
import com.tzy.sky.dto.ReplayResponseDTO;
import com.tzy.sky.dto.StreamRequestDTO;
import com.tzy.sky.service.ReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/replay")
@Tag(name = "回放管理", description = "卫星数据回放查询、流式推送等功能")
public class ReplayController {

    @Autowired
    private ReplayService replayService;

    @Operation(summary = "批量查询回放数据", description = "从降采样表查询指定时间范围的卫星数据")
    @PostMapping("/query")
    public ResponseEntity<ReplayResponseDTO> queryReplayData(@RequestBody ReplayQueryDTO queryDTO) {
        if (queryDTO.getSatelliteId() == null) {
            return ResponseEntity.badRequest().build();
        }

        if (queryDTO.getStartTime() == null) {
            queryDTO.setStartTime(LocalDateTime.now().minusDays(1));
        }
        if (queryDTO.getEndTime() == null) {
            queryDTO.setEndTime(LocalDateTime.now());
        }

        if (queryDTO.getInterval() != null && queryDTO.getInterval() > 1) {
            log.warn("降采样表已经是1Hz数据，interval参数被忽略");
        }

        ReplayResponseDTO response = replayService.queryReplayData(queryDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "快速查询回放数据", description = "GET方式快速查询指定卫星的数据")
    @GetMapping("/data/{satelliteId}")
    public ResponseEntity<ReplayResponseDTO> quickQuery(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @PathVariable String satelliteId,
            @Parameter(description = "开始时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "指定返回字段")
            @RequestParam(required = false) List<String> fields,
            @Parameter(description = "返回条数限制")
            @RequestParam(required = false, defaultValue = "10000") Integer limit) {

        ReplayQueryDTO queryDTO = new ReplayQueryDTO();
        queryDTO.setSatelliteId(satelliteId);
        queryDTO.setStartTime(startTime);
        queryDTO.setEndTime(endTime);
        queryDTO.setFields(fields);
        queryDTO.setLimit(limit);

        return ResponseEntity.ok(replayService.queryReplayData(queryDTO));
    }

    @Operation(summary = "启动流式回放", description = "通过SSE方式流式推送卫星数据")
    @GetMapping("/stream/start")
    public SseEmitter startStreamReplay(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @RequestParam String satelliteId,
            @Parameter(description = "开始仿真时间", required = true)
            @RequestParam Double startSimulationTime,
            @Parameter(description = "播放速度倍率")
            @RequestParam(required = false, defaultValue = "1.0") Double speed,
            @Parameter(description = "缓冲区大小(条)")
            @RequestParam(required = false, defaultValue = "60") Integer bufferSize) {

        StreamRequestDTO request = new StreamRequestDTO();
        request.setSatelliteId(satelliteId);
        request.setStartSimulationTime(startSimulationTime);
        request.setSpeed(speed);
        request.setBufferSize(bufferSize * 20);

        log.info("启动流式回放: satelliteId={}, startTime={}, speed={}x",
                satelliteId, startSimulationTime, speed);

        return replayService.startStreamReplay(request);
    }

    @Operation(summary = "停止流式回放")
    @PostMapping("/stream/{streamId}/stop")
    public ResponseEntity<Map<String, Object>> stopStream(
            @Parameter(description = "流ID")
            @PathVariable String streamId) {
        replayService.stopStream(streamId);

        Map<String, Object> result = new HashMap<>();
        result.put("streamId", streamId);
        result.put("status", "stopped");
        result.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "获取可用卫星列表")
    @GetMapping("/satellites")
    public ResponseEntity<List<String>> getSatellites() {
        return ResponseEntity.ok(replayService.getAvailableSatellites());
    }

    @Operation(summary = "获取数据时间范围", description = "查询指定卫星数据的时间范围")
    @GetMapping("/timerange/{satelliteId}")
    public ResponseEntity<Map<String, Object>> getTimeRange(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @PathVariable String satelliteId) {
        Map<String, Object> range = replayService.getTimeRange(satelliteId);
        range.put("dataSource", "satellite_data_sampled (1Hz)");
        return ResponseEntity.ok(range);
    }

    @Operation(summary = "获取数据统计", description = "查询指定卫星的数据记录统计")
    @GetMapping("/statistics/{satelliteId}")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @Parameter(description = "卫星ID", required = true, example = "SAT-001")
            @PathVariable String satelliteId) {
        Map<String, Object> stats = replayService.getStatistics(satelliteId);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "健康检查", description = "检查回放服务状态")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "replay-service");
        health.put("dataSource", "satellite_data_sampled (1Hz)");
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }
}