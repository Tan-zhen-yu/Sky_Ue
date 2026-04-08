// entity/SatelliteConfig.java - 卫星静态配置实体
package com.tzy.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 卫星静态配置实体类
 * 对应数据库表 satellite_config，存储卫星的轨道参数等静态配置
 * 替代 application.yml 中的 satellites 配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("satellite_config")
public class SatelliteConfig {

    /**
     * 卫星ID，如 Sat_01
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 卫星名称
     */
    private String name;

    /**
     * 轨道半径(km)
     */
    private Double orbitRadius;

    /**
     * 初始相位(度)
     */
    private Double startPhase;

    /**
     * 轨道倾角(度)
     */
    private Double inclination;

    /**
     * 卫星描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
