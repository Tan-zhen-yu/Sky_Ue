package com.tzy.sky.service;

import com.tzy.sky.config.properties.SatelliteSimulationProperties;
import com.tzy.sky.dto.base.Result;
import com.tzy.sky.entity.SatelliteConfig;
import com.tzy.sky.entity.GroundStationConfig;
import com.tzy.sky.handler.MyWebSocketHandler;
import com.tzy.sky.model.SatelliteModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("物理引擎服务单元测试")
class PhysicsServiceTest {

    @Mock
    private MyWebSocketHandler webSocketHandler;

    @Mock
    private SatelliteDataPersistenceService persistenceService;

    @Mock
    private SatelliteConfigService satelliteConfigService;

    @Mock
    private GroundStationConfigService groundStationConfigService;

    @Mock
    private OrbitDynamicsService orbitDynamicsService;

    @Mock
    private EnergyThermalService energyThermalService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private UeProcessService ueProcessService;

    private ObjectMapper objectMapper;

    private SatelliteSimulationProperties config;
    private PhysicsService physicsService;

    @BeforeEach
    void setUp() throws Exception {
        config = createTestConfig();
        objectMapper = new ObjectMapper();

        physicsService = new PhysicsService(
            webSocketHandler,
            config,
            persistenceService,
            satelliteConfigService,
            groundStationConfigService,
            orbitDynamicsService,
            energyThermalService,
            connectionService,
            ueProcessService,
            objectMapper
        );
    }

    private SatelliteSimulationProperties createTestConfig() {
        SatelliteSimulationProperties props = new SatelliteSimulationProperties();

        SatelliteSimulationProperties.PhysicsConfig physics = new SatelliteSimulationProperties.PhysicsConfig();

        SatelliteSimulationProperties.PhysicsConfig.EarthConfig earth = new SatelliteSimulationProperties.PhysicsConfig.EarthConfig();
        earth.setRadiusKm(6371.0);
        earth.setGm(398600.0);
        physics.setEarth(earth);

        SatelliteSimulationProperties.PhysicsConfig.SimulationConfig simulation = new SatelliteSimulationProperties.PhysicsConfig.SimulationConfig();
        simulation.setTickRateHz(20);
        simulation.setTimeScale(1.0);
        physics.setSimulation(simulation);

        SatelliteSimulationProperties.PhysicsConfig.ConnectionConfig connection = new SatelliteSimulationProperties.PhysicsConfig.ConnectionConfig();
        connection.setLowOrbitThreshold(10000.0);
        connection.setLowOrbitDistance(6000.0);
        connection.setHighOrbitDistance(50000.0);
        physics.setConnection(connection);

        SatelliteSimulationProperties.PhysicsConfig.EnergyConfig energy = new SatelliteSimulationProperties.PhysicsConfig.EnergyConfig();
        energy.setBaseDrainRate(0.05);
        energy.setSolarChargeRate(0.5);
        energy.setThermalDrainRate(0.3);
        energy.setPhotoBatteryThreshold(10.0);
        physics.setEnergy(energy);

        SatelliteSimulationProperties.PhysicsConfig.PropulsionConfig propulsion = new SatelliteSimulationProperties.PhysicsConfig.PropulsionConfig();
        propulsion.setEngineWarmupTime(3.0);
        propulsion.setFuelConsumptionRate(2.0);
        propulsion.setOrbitChangeRate(10.0);
        physics.setPropulsion(propulsion);

        SatelliteSimulationProperties.PhysicsConfig.ThermalConfig thermal = new SatelliteSimulationProperties.PhysicsConfig.ThermalConfig();
        thermal.setShadowTemperature(-100.0);
        thermal.setSunTemperature(150.0);
        thermal.setEngineWarmupHeat(50.0);
        thermal.setEngineActiveHeat(300.0);
        thermal.setHeaterTarget(200.0);
        thermal.setCoolerTarget(-50.0);
        thermal.setTemperatureChangeRate(0.1);
        physics.setThermal(thermal);

        props.setPhysics(physics);

        SatelliteSimulationProperties.UE5Config ue5 = new SatelliteSimulationProperties.UE5Config();
        SatelliteSimulationProperties.UE5Config.SignallingConfig signalling = new SatelliteSimulationProperties.UE5Config.SignallingConfig();
        signalling.setScriptPath("test-signalling-path");
        signalling.setStartupDelay(2);
        ue5.setSignalling(signalling);

        SatelliteSimulationProperties.UE5Config.ExecutableConfig executable = new SatelliteSimulationProperties.UE5Config.ExecutableConfig();
        executable.setPath("test-ue-path");
        executable.setPixelStreamingUrl("ws://127.0.0.1:8888");
        ue5.setExecutable(executable);

        props.setUe5(ue5);

        return props;
    }

    private List<SatelliteConfig> createMockSatelliteConfigs() {
        SatelliteConfig sat1 = new SatelliteConfig();
        sat1.setId("SAT-001");
        sat1.setOrbitRadius(7000.0);
        sat1.setStartPhase(0.0);
        sat1.setInclination(90.0);

        SatelliteConfig sat2 = new SatelliteConfig();
        sat2.setId("SAT-002");
        sat2.setOrbitRadius(8000.0);
        sat2.setStartPhase(120.0);
        sat2.setInclination(45.0);

        return Arrays.asList(sat1, sat2);
    }

    private List<GroundStationConfig> createMockGroundStationConfigs() {
        GroundStationConfig gs1 = new GroundStationConfig();
        gs1.setName("Beijing");
        gs1.setX(0.0);
        gs1.setY(6371.0);
        gs1.setZ(0.0);

        return Arrays.asList(gs1);
    }

    @Nested
    @DisplayName("初始化测试")
    class InitTests {

        @Test
        @DisplayName("配置对象为空时应抛出异常")
        void init_WithNullConfig_ShouldThrowException() {
            ReflectionTestUtils.setField(physicsService, "config", null);

            assertThrows(RuntimeException.class, () -> physicsService.init());
        }

        @Test
        @DisplayName("UE5配置为空时应抛出异常")
        void init_WithNullUE5Config_ShouldThrowException() {
            config.setUe5(null);

            assertThrows(RuntimeException.class, () -> physicsService.init());
        }

        @Test
        @DisplayName("正常初始化应成功")
        void init_WithValidConfig_ShouldSucceed() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());

            assertDoesNotThrow(() -> physicsService.init());
        }
    }

    @Nested
    @DisplayName("时间流速控制测试")
    class TimeScaleTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("设置时间流速应生效")
        void setTimeScale_ShouldUpdateValue() {
            physicsService.setTimeScale(2.0);
            assertEquals(2.0, physicsService.getTimeScale());
        }

        @Test
        @DisplayName("时间流速为零时应暂停仿真")
        void setTimeScale_ToZero_ShouldPauseSimulation() {
            physicsService.setTimeScale(0.0);
            assertEquals(0.0, physicsService.getTimeScale());
        }

        @Test
        @DisplayName("负时间流速应生效（倒退）")
        void setTimeScale_Negative_ShouldClampToZero() {
            physicsService.setTimeScale(-1.0);
            assertEquals(0.0, physicsService.getTimeScale());
        }
    }

    @Nested
    @DisplayName("热控模式控制测试")
    class ThermalModeTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("切换热控模式为加热器应生效")
        void setThermalMode_ToHeater_ShouldWork() {
            physicsService.setThermalMode("SAT-001", "heater");
            assertEquals("heater", physicsService.getSatellite("SAT-001").getThermalMode());
        }

        @Test
        @DisplayName("切换热控模式为制冷器应生效")
        void setThermalMode_ToCooler_ShouldWork() {
            physicsService.setThermalMode("SAT-001", "cooler");
            assertEquals("cooler", physicsService.getSatellite("SAT-001").getThermalMode());
        }

        @Test
        @DisplayName("切换热控模式为自动应生效")
        void setThermalMode_ToAuto_ShouldWork() {
            physicsService.setThermalMode("SAT-001", "heater");
            physicsService.setThermalMode("SAT-001", "auto");
            assertEquals("auto", physicsService.getSatellite("SAT-001").getThermalMode());
        }

        @Test
        @DisplayName("对不存在的卫星设置热控模式应不抛异常")
        void setThermalMode_ForNonExistentSatellite_ShouldNotThrow() {
            assertDoesNotThrow(() -> physicsService.setThermalMode("NON-EXISTENT", "heater"));
        }
    }

    @Nested
    @DisplayName("指向模式控制测试")
    class PointingModeTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("切换指向模式为对日应生效")
        void setPointingMode_ToSun_ShouldWork() {
            physicsService.setPointingMode("SAT-001", "sun");
            assertEquals("sun", physicsService.getSatellite("SAT-001").getPointingMode());
        }

        @Test
        @DisplayName("切换指向模式为对地应生效")
        void setPointingMode_ToEarth_ShouldWork() {
            physicsService.setPointingMode("SAT-001", "earth");
            assertEquals("earth", physicsService.getSatellite("SAT-001").getPointingMode());
        }

        @Test
        @DisplayName("切换指向模式为自由应生效")
        void setPointingMode_ToFree_ShouldWork() {
            physicsService.setPointingMode("SAT-001", "free");
            assertEquals("free", physicsService.getSatellite("SAT-001").getPointingMode());
        }
    }

    @Nested
    @DisplayName("轨道控制测试")
    class OrbitTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("设置轨道半径应生效")
        void setOrbit_ShouldUpdateRadius() {
            physicsService.setOrbit("SAT-001", 8000.0);
            assertEquals(8000.0, physicsService.getSatellite("SAT-001").getOrbitRadius());
        }

        @Test
        @DisplayName("对不存在的卫星设置轨道应不抛异常")
        void setOrbit_ForNonExistentSatellite_ShouldNotThrow() {
            assertDoesNotThrow(() -> physicsService.setOrbit("NON-EXISTENT", 8000.0));
        }
    }

    @Nested
    @DisplayName("姿态控制测试")
    class AttitudeTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("设置俯仰角应生效")
        void setAttitude_Pitch_ShouldWork() {
            physicsService.setAttitude("SAT-001", "pitch", 45.0);
            assertEquals(45.0, physicsService.getSatellite("SAT-001").getManualPitch());
        }

        @Test
        @DisplayName("设置偏航角应生效")
        void setAttitude_Yaw_ShouldWork() {
            physicsService.setAttitude("SAT-001", "yaw", 30.0);
            assertEquals(30.0, physicsService.getSatellite("SAT-001").getManualYaw());
        }

        @Test
        @DisplayName("设置翻滚角应生效")
        void setAttitude_Roll_ShouldWork() {
            physicsService.setAttitude("SAT-001", "roll", 15.0);
            assertEquals(15.0, physicsService.getSatellite("SAT-001").getManualRoll());
        }

        @Test
        @DisplayName("设置未知旋转轴应不抛异常")
        void setAttitude_UnknownAxis_ShouldNotThrow() {
            assertDoesNotThrow(() -> physicsService.setAttitude("SAT-001", "unknown", 10.0));
        }
    }

    @Nested
    @DisplayName("发动机控制测试")
    class EngineTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("发动机启动应生效")
        void setEngineState_Active_ShouldWork() {
            physicsService.setEngineState("SAT-001", true);
            assertTrue(physicsService.getSatellite("SAT-001").isEngineActive());
        }

        @Test
        @DisplayName("发动机熄火应生效")
        void setEngineState_Inactive_ShouldWork() {
            physicsService.setEngineState("SAT-001", true);
            physicsService.setEngineState("SAT-001", false);
            assertFalse(physicsService.getSatellite("SAT-001").isEngineActive());
        }
    }

    @Nested
    @DisplayName("传感器状态测试")
    class SensorTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("更新传感器遮挡状态应生效")
        void updateSensorStatus_Occluded_ShouldWork() {
            physicsService.updateSensorStatus("SAT-001", true);
            assertTrue(physicsService.getSatellite("SAT-001").isOccluded());
        }

        @Test
        @DisplayName("更新传感器非遮挡状态应生效")
        void updateSensorStatus_NotOccluded_ShouldWork() {
            physicsService.getSatellite("SAT-001").setOccluded(true);
            physicsService.updateSensorStatus("SAT-001", false);
            assertFalse(physicsService.getSatellite("SAT-001").isOccluded());
        }
    }

    @Nested
    @DisplayName("速度控制测试")
    class SpeedTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("设置卫星速度应生效")
        void setSpeed_ShouldWork() {
            physicsService.setSpeed("SAT-001", 7.8);
            assertEquals(7.8, physicsService.getSatellite("SAT-001").getSpeed());
        }
    }

    @Nested
    @DisplayName("拍照指令测试")
    class CameraTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("电量充足时拍照应返回成功消息")
        void triggerCamera_WithEnoughBattery_ShouldReturnSuccess() {
            physicsService.getSatellite("SAT-001").setBattery(100.0);
            String result = physicsService.triggerCamera("SAT-001");
            assertTrue(result.contains("成功") || result.contains("IMG-"));
        }

        @Test
        @DisplayName("电量不足时拍照应返回警告消息")
        void triggerCamera_WithLowBattery_ShouldReturnWarning() {
            physicsService.getSatellite("SAT-001").setBattery(5.0);
            String result = physicsService.triggerCamera("SAT-001");
            assertTrue(result.contains("电量不足") || result.contains("⚠️"));
        }

        @Test
        @DisplayName("对不存在的卫星拍照应返回错误消息")
        void triggerCamera_NonExistentSatellite_ShouldReturnError() {
            String result = physicsService.triggerCamera("NON-EXISTENT");
            assertEquals("卫星不存在", result);
        }
    }

    @Nested
    @DisplayName("数据持久化控制测试")
    class DataPersistenceTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("默认应启用数据持久化")
        void defaultState_ShouldBeEnabled() {
            assertTrue(physicsService.isDataPersistenceEnabled());
        }

        @Test
        @DisplayName("禁用数据持久化应生效")
        void disableDataPersistence_ShouldWork() {
            physicsService.disableDataPersistence();
            assertFalse(physicsService.isDataPersistenceEnabled());
        }

        @Test
        @DisplayName("启用数据持久化应生效")
        void enableDataPersistence_ShouldWork() {
            physicsService.disableDataPersistence();
            physicsService.enableDataPersistence();
            assertTrue(physicsService.isDataPersistenceEnabled());
        }
    }

    @Nested
    @DisplayName("仿真状态查询测试")
    class StateQueryTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("获取所有卫星应返回Map")
        void getAllSatellites_ShouldReturnMap() {
            assertNotNull(physicsService.getAllSatellites());
        }

        @Test
        @DisplayName("获取存在的卫星应返回对象")
        void getSatellite_Existing_ShouldReturnModel() {
            SatelliteModel sat = physicsService.getSatellite("SAT-001");
            assertNotNull(sat);
            assertEquals("SAT-001", sat.getId());
        }

        @Test
        @DisplayName("获取不存在的卫星应返回null")
        void getSatellite_NonExistent_ShouldReturnNull() {
            assertNull(physicsService.getSatellite("NON-EXISTENT"));
        }

        @Test
        @DisplayName("获取仿真时间应返回非负值")
        void getSimulationTime_ShouldReturnNonNegative() {
            assertTrue(physicsService.getSimulationTime() >= 0.0);
        }

        @Test
        @DisplayName("获取时间流速应返回非负值")
        void getTimeScale_ShouldReturnNonNegative() {
            assertTrue(physicsService.getTimeScale() >= 0.0);
        }
    }

    @Nested
    @DisplayName("UE系统控制测试")
    class UESystemTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("启动UE系统应返回成功结果")
        void startUeSystem_ShouldReturnSuccess() {
            when(ueProcessService.startUeSystem()).thenReturn(Result.success("UE系统启动成功"));
            Result<String> result = physicsService.startUeSystem();
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }

        @Test
        @DisplayName("停止UE系统应返回成功结果")
        void stopUeSystem_ShouldReturnSuccess() {
            when(ueProcessService.stopUeSystem()).thenReturn(Result.success("UE系统已停止"));
            Result<String> result = physicsService.stopUeSystem();
            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    @Nested
    @DisplayName("真实姿态更新测试")
    class RealRotationTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("更新真实姿态应生效")
        void updateRealRotation_ShouldWork() {
            physicsService.updateRealRotation("SAT-001", 10.0, 20.0, 30.0);
            assertEquals(10.0, physicsService.getSatellite("SAT-001").getRealPitch());
            assertEquals(20.0, physicsService.getSatellite("SAT-001").getRealYaw());
            assertEquals(30.0, physicsService.getSatellite("SAT-001").getRealRoll());
        }
    }

    @Nested
    @DisplayName("聚焦模式测试")
    class FocusModeTests {

        @BeforeEach
        void initService() {
            when(satelliteConfigService.hasSatelliteConfigs()).thenReturn(true);
            when(satelliteConfigService.getAllEnabledSatellites()).thenReturn(createMockSatelliteConfigs());
            when(groundStationConfigService.hasStationConfigs()).thenReturn(true);
            when(groundStationConfigService.getAllEnabledStations()).thenReturn(createMockGroundStationConfigs());
            physicsService.init();
        }

        @Test
        @DisplayName("设置聚焦目标应返回成功消息")
        void setFocusTarget_ShouldReturnSuccess() {
            String result = physicsService.setFocusTarget("SAT-001");
            assertTrue(result.contains("Focus") || result.contains("SAT-001"));
        }

        @Test
        @DisplayName("对不存在的卫星设置聚焦应返回错误")
        void setFocusTarget_NonExistent_ShouldReturnError() {
            String result = physicsService.setFocusTarget("NON-EXISTENT");
            assertEquals("卫星不存在", result);
        }

        @Test
        @DisplayName("viewEarth应正常执行")
        void viewEarth_ShouldWork() {
            assertDoesNotThrow(() -> physicsService.viewEarth());
        }
    }
}