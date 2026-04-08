package com.tzy.sky.dto.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tzy.sky.dto.base.LocationDTO;
import com.tzy.sky.dto.base.RotationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SatelliteSyncPayload {

    private String id;

    // 1. 位置与姿态
    private LocationDTO loc;
    private RotationDTO rot;

    // 2. 能源系统
    @JsonProperty("bat")
    private double batteryLevel;

    private String state; // "charging" / "discharging"

    // === 新增字段 (为了支持新功能) ===

    // 3. 热控系统
    @JsonProperty("temp")
    private double temperature;  // 实时温度 (用于驱动 UE 材质颜色 & Web 图表)

    @JsonProperty("thermal")
    private String thermalMode;  // "auto", "heater", "cooler" (用于 Web 显示当前控温策略)

    // 4. 指向与姿态模式
    @JsonProperty("mode")
    private String pointingMode; // "earth", "sun", "free" (用于 UE 判断该朝向哪里)

    // 5. 载荷状态 (拍照/扫描)
    @JsonProperty("scan")
    private boolean isScanning;  // true=开启圆锥体, false=隐藏 (用于 UE 控制 Cone 可见性)
    private double speed;
    private String connectedStation;
    private double solarEfficiency;
    private boolean engineActive;
}