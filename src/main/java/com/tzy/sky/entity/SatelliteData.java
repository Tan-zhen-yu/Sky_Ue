// entity/SatelliteData.java - 完整数据表
package com.tzy.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("satellite_data")
public class SatelliteData {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String satelliteId;
    private Double simulationTime;
    private LocalDateTime timestamp;

    // 位置
    private Double positionX;
    private Double positionY;
    private Double positionZ;
    private Double orbitRadius;

    // 姿态
    private Double realPitch;
    private Double realRoll;
    private Double realYaw;

    // 能源
    private Double battery;
    private Double solarEfficiency;

    // 热控
    private Double temperature;
    private String thermalMode;

    // 状态
    private Boolean engineActive;
    private Boolean isOccluded;
    private Boolean isScanning;
    private String pointingMode;
    private String connectedStation;

    // 动力学
    private Double speed;

    // 元数据
    private LocalDateTime createdAt;
}