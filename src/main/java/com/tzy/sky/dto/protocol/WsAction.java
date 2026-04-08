package com.tzy.sky.dto.protocol;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WsAction {
    SYNC("sync"),
    SENSOR_REPORT("sensor_report"),
    UPDATE_BATTERY("update_battery"),
    ENGINE_STATE("engine_state"),
    // === 新增 ===
    SET_TIME_SCALE("set_time_scale"), // 设置时间流速
    STATE_CHANGE("state_change"),
    TAKE_PHOTO("take_photo"),
    FOCUS_MODE("focus_mode");


    private final String value;

    WsAction(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}