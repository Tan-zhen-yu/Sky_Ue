package com.tzy.sky.dto;


public class StreamRequestDTO {
    private String satelliteId;
    private Double startSimulationTime;
    private Double speed = 1.0; // 播放倍速
    private Integer bufferSize = 1000; // 缓冲区大小
    private String format = "json"; // json 或 protobuf

    // Getters and Setters
    public String getSatelliteId() { return satelliteId; }
    public void setSatelliteId(String satelliteId) { this.satelliteId = satelliteId; }

    public Double getStartSimulationTime() { return startSimulationTime; }
    public void setStartSimulationTime(Double startSimulationTime) { this.startSimulationTime = startSimulationTime; }

    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }

    public Integer getBufferSize() { return bufferSize; }
    public void setBufferSize(Integer bufferSize) { this.bufferSize = bufferSize; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}