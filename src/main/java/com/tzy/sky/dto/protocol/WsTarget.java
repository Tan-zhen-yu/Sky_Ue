package com.tzy.sky.dto.protocol;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WsTarget {
    SERVER("server"),
    SAT("sat"),
    CLIENTS("clients");

    private final String value;

    WsTarget(String value) {
        this.value = value;
    }

    // 序列化时直接输出 "server" 而不是 "SERVER"
    @JsonValue
    public String getValue() {
        return value;
    }
}