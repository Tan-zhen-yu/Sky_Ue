// controller/DataPersistenceController.java
package com.tzy.sky.controller;

import com.tzy.sky.service.DataBuffer;
import com.tzy.sky.service.PhysicsService;
import com.tzy.sky.service.SatelliteDataPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/persistence")
@CrossOrigin(origins = "*")
public class DataPersistenceController {

    @Autowired
    private PhysicsService physicsService;

    @Autowired
    private SatelliteDataPersistenceService persistenceService;

    /**
     * 启用数据保存
     */
    @PostMapping("/enable")
    public Map<String, Object> enablePersistence() {
        physicsService.enableDataPersistence();
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "数据保存已启用");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 禁用数据保存
     */
    @PostMapping("/disable")
    public Map<String, Object> disablePersistence() {
        physicsService.disableDataPersistence();
        // 立即刷新缓冲区
        DataBuffer.getInstance().flush();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "数据保存已禁用，缓冲区已刷新");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 立即刷新缓冲区
     */
    @PostMapping("/flush")
    public Map<String, Object> flushBuffer() {
        DataBuffer.getInstance().flush();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "缓冲区已手动刷新");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }


}