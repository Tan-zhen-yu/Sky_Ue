package com.tzy.sky.dto;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@Data
public class ReplayQueryDTO {
    private String satelliteId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double startSimulationTime;
    private Double endSimulationTime;
    private List<String> fields; // 指定返回的字段，用于性能优化
    private Integer interval; // 数据采样间隔（秒），用于降采样
    private Integer limit; // 最大返回条数


    }