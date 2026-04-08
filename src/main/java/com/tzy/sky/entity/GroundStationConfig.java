// entity/GroundStationConfig.java - 地面站配置实体
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
 * 地面站静态配置实体类
 * 对应数据库表 ground_station_config，存储地面站坐标等静态配置
 * 替代 application.yml 中的 groundStations 配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ground_station_config")
public class GroundStationConfig {

    /**
     * 地面站ID，如 GS_01
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 地面站名称
     */
    private String name;

    /**
     * X坐标
     */
    private Double x;

    /**
     * Y坐标
     */
    private Double y;

    /**
     * Z坐标
     */
    private Double z;

    /**
     * 地面站描述
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
