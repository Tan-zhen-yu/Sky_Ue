-- schema.sql - Spring Boot 启动时自动执行
-- 创建卫星静态配置表

CREATE TABLE IF NOT EXISTS satellite_config (
    id VARCHAR(50) PRIMARY KEY COMMENT '卫星ID，如 Sat_01',
    name VARCHAR(100) COMMENT '卫星名称',
    orbit_radius DOUBLE NOT NULL COMMENT '轨道半径(km)',
    start_phase DOUBLE NOT NULL COMMENT '初始相位(度)',
    inclination DOUBLE NOT NULL COMMENT '轨道倾角(度)',
    description VARCHAR(255) COMMENT '卫星描述',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);
