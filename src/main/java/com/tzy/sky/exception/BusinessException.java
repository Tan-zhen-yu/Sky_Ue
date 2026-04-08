package com.tzy.sky.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public static BusinessException satelliteNotFound(String satelliteId) {
        return new BusinessException(404, "卫星不存在: " + satelliteId);
    }

    public static BusinessException invalidParameter(String message) {
        return new BusinessException(400, "参数错误: " + message);
    }

    public static BusinessException operationFailed(String message) {
        return new BusinessException(500, "操作失败: " + message);
    }

    public static BusinessException lowBattery() {
        return new BusinessException(400, "电量不足，无法执行操作");
    }

    public static BusinessException groundStationNotFound(String stationName) {
        return new BusinessException(404, "地面站不存在: " + stationName);
    }
}