package com.tzy.sky.dto.payload;

import lombok.Data;

@Data
public class SensorReportPayload {
    private String id;
    private boolean occluded;
}