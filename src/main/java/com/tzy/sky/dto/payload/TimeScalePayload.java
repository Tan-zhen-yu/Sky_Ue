package com.tzy.sky.dto.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeScalePayload {
    // 时间倍率 (例如: 1.0 是正常, 10.0 是十倍速, 0.0 是暂停)
    private double scale;
}