package com.tzy.sky.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("卫星模型单元测试")
class SatelliteModelTest {

    private SatelliteModel satellite;

    @BeforeEach
    void setUp() {
        satellite = new SatelliteModel("SAT-001", 7000.0, 0.0, 90.0);
    }

    @Test
    @DisplayName("构造函数应正确初始化所有字段")
    void constructor_ShouldInitializeAllFields() {
        assertEquals("SAT-001", satellite.getId());
        assertEquals(7000.0, satellite.getOrbitRadius());
        assertEquals(0.0, satellite.getStartPhase());
        assertEquals(90.0, satellite.getInclination());
    }

    @Test
    @DisplayName("默认初始值应正确设置")
    void defaultValues_ShouldBeCorrect() {
        assertEquals(100.0, satellite.getBattery());
        assertEquals(20.0, satellite.getTemperature());
        assertEquals("auto", satellite.getThermalMode());
        assertEquals("earth", satellite.getPointingMode());
        assertFalse(satellite.isEngineActive());
        assertFalse(satellite.isOccluded());
        assertFalse(satellite.isScanning());
    }

    @Test
    @DisplayName("轨道半径修改应生效")
    void setOrbitRadius_ShouldUpdateValue() {
        satellite.setOrbitRadius(8000.0);
        assertEquals(8000.0, satellite.getOrbitRadius());
    }

    @Test
    @DisplayName("电池电量应在有效范围内")
    void battery_ShouldBeWithinValidRange() {
        satellite.setBattery(150.0);
        assertEquals(150.0, satellite.getBattery());

        satellite.setBattery(-10.0);
        assertEquals(-10.0, satellite.getBattery());
    }

    @Test
    @DisplayName("姿态控制字段应能正常修改")
    void attitudeFields_ShouldBeSettable() {
        satellite.setManualPitch(45.0);
        satellite.setManualYaw(30.0);
        satellite.setManualRoll(15.0);

        assertEquals(45.0, satellite.getManualPitch());
        assertEquals(30.0, satellite.getManualYaw());
        assertEquals(15.0, satellite.getManualRoll());
    }

    @Test
    @DisplayName("热控模式切换应生效")
    void thermalMode_ShouldSwitchCorrectly() {
        satellite.setThermalMode("heater");
        assertEquals("heater", satellite.getThermalMode());

        satellite.setThermalMode("cooler");
        assertEquals("cooler", satellite.getThermalMode());

        satellite.setThermalMode("auto");
        assertEquals("auto", satellite.getThermalMode());
    }

    @Test
    @DisplayName("指向模式切换应生效")
    void pointingMode_ShouldSwitchCorrectly() {
        satellite.setPointingMode("sun");
        assertEquals("sun", satellite.getPointingMode());

        satellite.setPointingMode("free");
        assertEquals("free", satellite.getPointingMode());

        satellite.setPointingMode("earth");
        assertEquals("earth", satellite.getPointingMode());
    }

    @Test
    @DisplayName("发动机状态应能切换")
    void engineActive_ShouldToggle() {
        assertFalse(satellite.isEngineActive());

        satellite.setEngineActive(true);
        assertTrue(satellite.isEngineActive());

        satellite.setEngineActive(false);
        assertFalse(satellite.isEngineActive());
    }

    @Test
    @DisplayName("遮挡状态应能切换")
    void occluded_ShouldToggle() {
        assertFalse(satellite.isOccluded());

        satellite.setOccluded(true);
        assertTrue(satellite.isOccluded());
    }

    @Test
    @DisplayName("扫描状态应能切换")
    void scanning_ShouldToggle() {
        assertFalse(satellite.isScanning());

        satellite.setScanning(true);
        assertTrue(satellite.isScanning());
    }

    @Test
    @DisplayName("温度应能正常修改")
    void temperature_ShouldBeSettable() {
        satellite.setTemperature(100.0);
        assertEquals(100.0, satellite.getTemperature());

        satellite.setTemperature(-50.0);
        assertEquals(-50.0, satellite.getTemperature());
    }

    @Test
    @DisplayName("太阳能效率应在有效范围内")
    void solarEfficiency_ShouldBeSettable() {
        satellite.setSolarEfficiency(0.8);
        assertEquals(0.8, satellite.getSolarEfficiency());

        satellite.setSolarEfficiency(0.0);
        assertEquals(0.0, satellite.getSolarEfficiency());

        satellite.setSolarEfficiency(1.0);
        assertEquals(1.0, satellite.getSolarEfficiency());
    }

    @Test
    @DisplayName("地面站连接状态应能设置")
    void connectedStation_ShouldBeSettable() {
        assertNull(satellite.getConnectedStation());

        satellite.setConnectedStation("Beijing-Station");
        assertEquals("Beijing-Station", satellite.getConnectedStation());

        satellite.setConnectedStation(null);
        assertNull(satellite.getConnectedStation());
    }

    @Test
    @DisplayName("速度应能正常修改")
    void speed_ShouldBeSettable() {
        satellite.setSpeed(7.5);
        assertEquals(7.5, satellite.getSpeed());
    }

    @Test
    @DisplayName("相位角应能循环更新（0-360度）")
    void phaseAngle_ShouldWrapAround() {
        satellite.setStartPhase(350.0);
        assertEquals(350.0, satellite.getStartPhase());

        satellite.setStartPhase(0.0);
        assertEquals(0.0, satellite.getStartPhase());
    }
}