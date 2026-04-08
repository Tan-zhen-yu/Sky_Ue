package com.tzy.sky.dto;



import java.util.List;
import java.util.Map;

public class ReplayResponseDTO {
    private String satelliteId;
    private Long totalCount;
    private Long returnedCount;
    private List<Map<String, Object>> data; // 动态字段返回
    private Long startTimestamp;
    private Long endTimestamp;
    private String nextCursor; // 用于分页

    // Getters and Setters
    public String getSatelliteId() { return satelliteId; }
    public void setSatelliteId(String satelliteId) { this.satelliteId = satelliteId; }

    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }

    public Long getReturnedCount() { return returnedCount; }
    public void setReturnedCount(Long returnedCount) { this.returnedCount = returnedCount; }

    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }

    public Long getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(Long startTimestamp) { this.startTimestamp = startTimestamp; }

    public Long getEndTimestamp() { return endTimestamp; }
    public void setEndTimestamp(Long endTimestamp) { this.endTimestamp = endTimestamp; }

    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
}