// service/DataBuffer.java
package com.tzy.sky.service;

import com.tzy.sky.entity.SatelliteData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DataBuffer {

    private static final int BUFFER_SIZE = 500;
    private static final int FLUSH_INTERVAL_MS = 1000;

    private final List<SatelliteData> buffer = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static DataBuffer instance;
    private SatelliteDataPersistenceService persistenceService;

    private DataBuffer() {
        // 定时刷新
        scheduler.scheduleAtFixedRate(this::flush, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public static synchronized DataBuffer getInstance() {
        if (instance == null) {
            instance = new DataBuffer();
        }
        return instance;
    }

    public void setPersistenceService(SatelliteDataPersistenceService service) {
        this.persistenceService = service;
    }

    /**
     * 添加数据到缓冲
     */
    public void add(SatelliteData data, SatelliteDataPersistenceService service) {
        if (persistenceService == null) {
            persistenceService = service;
        }

        buffer.add(data);

        // 达到阈值立即刷新
        if (buffer.size() >= BUFFER_SIZE) {
            flush();
        }
    }

    /**
     * 刷新缓冲区到数据库
     */
    public synchronized void flush() {
        if (buffer.isEmpty() || persistenceService == null) return;

        List<SatelliteData> batch = new ArrayList<>(buffer);
        buffer.clear();

        try {
            persistenceService.saveBatch(batch);
            log.debug("缓冲区刷新: {} 条数据", batch.size());
        } catch (Exception e) {
            log.error("批量保存失败", e);
            // 失败时可以选择重试或记录到日志文件
        }
    }

    /**
     * 关闭时刷新剩余数据
     */
    public void shutdown() {
        flush();
        scheduler.shutdown();
    }
}