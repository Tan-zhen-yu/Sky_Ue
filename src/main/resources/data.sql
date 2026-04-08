-- data.sql - Spring Boot 启动时自动执行
-- 插入默认卫星配置

INSERT INTO satellite_config (id, name, orbit_radius, start_phase, inclination, description, enabled) VALUES
('Sat_01', '近地轨道卫星-01', 6500.0, 30.0, 60.0, '低轨道观测卫星', true),
('Sat_02', '地球同步卫星-02', 42164.0, 180.0, 0.0, '地球静止轨道通信卫星', true),
('Sat_03', '太阳同步卫星-03', 7200.0, 90.0, 98.0, '太阳同步轨道遥感卫星', true),
('Sat_04', '中地球轨道卫星-04', 26000.0, 270.0, 55.0, '中轨道导航卫星', true)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    orbit_radius = VALUES(orbit_radius),
    start_phase = VALUES(start_phase),
    inclination = VALUES(inclination),
    description = VALUES(description),
    enabled = VALUES(enabled);
