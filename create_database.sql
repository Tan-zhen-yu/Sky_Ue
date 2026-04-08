-- 创建卫星仿真数据库
CREATE DATABASE IF NOT EXISTS satellite_simulation 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE satellite_simulation;

-- 创建卫星数据表
CREATE TABLE IF NOT EXISTS satellite_data (
    id VARCHAR(255) PRIMARY KEY,
    satellite_id VARCHAR(50) NOT NULL,
    timestamp DATETIME NOT NULL,
    simulation_time DOUBLE,
    position_x DOUBLE,
    position_y DOUBLE,
    position_z DOUBLE,
    real_pitch DOUBLE,
    real_yaw DOUBLE,
    real_roll DOUBLE,
    manual_pitch DOUBLE,
    manual_yaw DOUBLE,
    manual_roll DOUBLE,
    orbit_radius DOUBLE,
    start_phase DOUBLE,
    inclination DOUBLE,
    speed DOUBLE,
    battery DOUBLE,
    solar_efficiency DOUBLE,
    is_occluded BOOLEAN,
    temperature DOUBLE,
    thermal_mode VARCHAR(20),
    pointing_mode VARCHAR(20),
    is_scanning BOOLEAN,
    engine_active BOOLEAN,
    connected_station VARCHAR(50),
    created_at DATETIME NOT NULL,
    INDEX idx_satellite_timestamp (satellite_id, timestamp),
    INDEX idx_timestamp (timestamp)
);

-- 显示表结构
DESCRIBE satellite_data;

-- ==========================================
-- 卫星静态配置表 (替代 application.yml 中的 satellites 配置)
-- ==========================================
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

-- 插入默认卫星配置 (从 application.yml 迁移)
INSERT INTO satellite_config (id, name, orbit_radius, start_phase, inclination, description) VALUES
('Sat_01', '近地轨道卫星-01', 6500.0, 30.0, 60.0, '低轨道观测卫星'),
('Sat_02', '地球同步卫星-02', 42164.0, 180.0, 0.0, '地球静止轨道通信卫星'),
('Sat_03', '太阳同步卫星-03', 7200.0, 90.0, 98.0, '太阳同步轨道遥感卫星'),
('Sat_04', '中地球轨道卫星-04', 26000.0, 270.0, 55.0, '中轨道导航卫星')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    orbit_radius = VALUES(orbit_radius),
    start_phase = VALUES(start_phase),
    inclination = VALUES(inclination),
    description = VALUES(description);

-- 显示卫星配置表结构
DESCRIBE satellite_config;

-- 查看卫星配置数据
SELECT * FROM satellite_config;

-- ==========================================
-- 地面站配置表 (替代 application.yml 中的 groundStations 配置)
-- ==========================================
CREATE TABLE IF NOT EXISTS ground_station_config (
    id VARCHAR(50) PRIMARY KEY COMMENT '地面站ID',
    name VARCHAR(100) NOT NULL COMMENT '地面站名称',
    x DOUBLE NOT NULL COMMENT 'X坐标',
    y DOUBLE NOT NULL COMMENT 'Y坐标',
    z DOUBLE NOT NULL COMMENT 'Z坐标',
    description VARCHAR(255) COMMENT '地面站描述',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 插入默认地面站配置 (从 application.yml 迁移)
INSERT INTO ground_station_config (id, name, x, y, z, description) VALUES
('GS_01', 'Beijing-Station', 4000, 2000, 4500, '北京地面站'),
('GS_02', 'Kashi-Station', 3000, 4000, 3000, '喀什地面站'),
('GS_03', 'Antarctic-Base', 0, 0, -6371, '南极基地'),
('GS_04', 'Singapore-Station', 4000, 4800, 0, '新加坡地面站'),
('GS_05', 'Australia-Perth', 2000, 5000, -3000, '澳大利亚珀斯站'),
('GS_06', 'USA-Hawaii', -5000, 2000, 2000, '美国夏威夷站'),
('GS_07', 'Pacific-Ship', -4000, -2000, 0, '太平洋船只'),
('GS_08', 'Africa-Base', 2000, -5000, 1000, '非洲基地')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    x = VALUES(x),
    y = VALUES(y),
    z = VALUES(z),
    description = VALUES(description);

-- 显示地面站配置表结构
DESCRIBE ground_station_config;

-- 查看地面站配置数据
SELECT * FROM ground_station_config;
