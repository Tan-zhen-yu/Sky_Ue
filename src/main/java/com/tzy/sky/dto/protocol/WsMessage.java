package com.tzy.sky.dto.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage<T> {

    // 目标路由
    private WsTarget target;

    // 动作指令
    private WsAction action;

    // 具体的业务数据 (泛型)
    private T payload;

    // 时间戳 (可选，工业场景常用于计算延迟)
    @Builder.Default
    private long timestamp = System.currentTimeMillis();
}