package com.tzy.sky.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 卫星领域模型 (Domain Model)
 * 用于在服务端内存中存储卫星的实时物理状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SatelliteModel {

    // 基础信息
    private String id;

    // 轨道参数
    private double orbitRadius;
    private double speed;      // 虽然目前计算没用到，但保留作为扩展
    private double startPhase; // 初始相位

    private double inclination; // 轨道倾角 (0=赤道, 90=极地, 98=太阳同步)

    // 姿态控制 (手动偏置量)
    private double manualPitch = 0.0;
    private double manualYaw = 0.0;
    private double manualRoll = 0.0;

    // 2. UE 回传的真实旋转值 (用于 Java -> 前端 遥测显示)
    private double realPitch;
    private double realYaw;
    private double realRoll;

    // 传感器与能源数据
    private double battery = 100.0; // 0.0 - 100.0
    private boolean isOccluded = false; // 是否被遮挡

    private double temperature = 20.0; // 初始温度
    private String thermalMode = "auto"; // auto, heater, cooler
    private String pointingMode = "earth"; // 当前指向哪里
    private boolean isScanning;  // true=开启圆锥体, false=隐藏 (用于 UE 控制 Cone 可见性)

    private String connectedStation; // 当前连接的地面站名称 (null代表断连)
    private double solarEfficiency;  // 光照效率 (0.0 - 1.0)
    private boolean engineActive;    // 发动机是否在点火

    // 构造函数：用于初始化固定参数
    public SatelliteModel(String id, double orbitRadius, double startPhase, double inclination) {
        this.id = id;
        this.orbitRadius = orbitRadius;
        this.startPhase = startPhase;
        this.inclination = inclination;

        // 默认值初始化
        this.battery = 100.0;
        this.temperature = 20.0;
        this.thermalMode = "auto";
        this.pointingMode = "earth"; // 默认对地

    }
}