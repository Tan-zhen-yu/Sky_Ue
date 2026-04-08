package com.tzy.sky.controller;

import com.tzy.sky.dto.base.Result;
import com.tzy.sky.model.SatelliteModel;
import com.tzy.sky.service.PhysicsService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("卫星控制器单元测试")
class SatControllerTest {

    @Mock
    private PhysicsService physicsService;

    @InjectMocks
    private SatController satController;

    @BeforeEach
    void setUp() {
    }

    @Nested
    @DisplayName("轨道控制测试")
    class OrbitTests {

        @Test
        @DisplayName("设置轨道半径应返回成功响应")
        void setOrbit_ShouldReturnSuccessResult() {
            Result<String> result = satController.setOrbit("SAT-001", 8000.0);

            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertTrue(result.getData().contains("SAT-001"));
            assertTrue(result.getData().contains("8000"));
            verify(physicsService).setOrbit("SAT-001", 8000.0);
        }

        @Test
        @DisplayName("轨道高度为负数应抛出异常")
        void setOrbit_WithNegativeAltitude_ShouldThrowException() {
            assertThrows(Exception.class, () -> satController.setOrbit("SAT-001", -100.0));
        }

        @Test
        @DisplayName("空卫星ID应抛出异常")
        void setOrbit_WithEmptyId_ShouldThrowException() {
            assertThrows(Exception.class, () -> satController.setOrbit("", 8000.0));
        }
    }

    @Nested
    @DisplayName("姿态控制测试")
    class AttitudeTests {

        @Test
        @DisplayName("设置俯仰角应返回成功响应")
        void setAttitude_Pitch_ShouldReturnSuccessResult() {
            Result<String> result = satController.setAttitude("SAT-001", "pitch", 45.0);

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("pitch"));
            assertTrue(result.getData().contains("45"));
            verify(physicsService).setAttitude("SAT-001", "pitch", 45.0);
        }

        @Test
        @DisplayName("设置偏航角应返回成功响应")
        void setAttitude_Yaw_ShouldReturnSuccessResult() {
            Result<String> result = satController.setAttitude("SAT-001", "yaw", 30.0);

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("yaw"));
            verify(physicsService).setAttitude("SAT-001", "yaw", 30.0);
        }

        @Test
        @DisplayName("设置翻滚角应返回成功响应")
        void setAttitude_Roll_ShouldReturnSuccessResult() {
            Result<String> result = satController.setAttitude("SAT-001", "roll", 15.0);

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("roll"));
            verify(physicsService).setAttitude("SAT-001", "roll", 15.0);
        }

        @Test
        @DisplayName("空旋转轴应抛出异常")
        void setAttitude_WithEmptyAxis_ShouldThrowException() {
            assertThrows(Exception.class, () -> satController.setAttitude("SAT-001", "", 45.0));
        }
    }

    @Nested
    @DisplayName("传感器状态测试")
    class SensorTests {

        @Test
        @DisplayName("报告遮挡状态应返回成功响应")
        void reportSensor_Occluded_ShouldReturnSuccess() {
            Result<String> result = satController.reportSensor("SAT-001", true);

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("Occluded") || result.getData().contains("Updated"));
            verify(physicsService).updateSensorStatus("SAT-001", true);
        }

        @Test
        @DisplayName("报告非遮挡状态应返回成功响应")
        void reportSensor_NotOccluded_ShouldReturnSuccess() {
            Result<String> result = satController.reportSensor("SAT-001", false);

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("Clear") || result.getData().contains("Updated"));
            verify(physicsService).updateSensorStatus("SAT-001", false);
        }
    }

    @Nested
    @DisplayName("速度控制测试")
    class SpeedTests {

        @Test
        @DisplayName("设置速度应返回成功响应")
        void setSpeed_ShouldReturnSuccessResult() {
            Result<String> result = satController.setSpeed("SAT-001", 7.8);

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("SAT-001"));
            assertTrue(result.getData().contains("7.8"));
            verify(physicsService).setSpeed("SAT-001", 7.8);
        }

        @Test
        @DisplayName("负速度应抛出异常")
        void setSpeed_WithNegative_ShouldThrowException() {
            assertThrows(Exception.class, () -> satController.setSpeed("SAT-001", -1.0));
        }
    }

    @Nested
    @DisplayName("发动机控制测试")
    class EngineTests {

        @Test
        @DisplayName("启动发动机应返回成功响应")
        void setEngine_Active_ShouldReturnSuccess() {
            Result<String> result = satController.setEngine("SAT-001", true);

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("Engine") || result.getData().contains("true"));
            verify(physicsService).setEngineState("SAT-001", true);
        }

        @Test
        @DisplayName("关闭发动机应返回成功响应")
        void setEngine_Inactive_ShouldReturnSuccess() {
            Result<String> result = satController.setEngine("SAT-001", false);

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("Engine") || result.getData().contains("false"));
            verify(physicsService).setEngineState("SAT-001", false);
        }
    }

    @Nested
    @DisplayName("拍照指令测试")
    class PhotoTests {

        @Test
        @DisplayName("拍照成功应返回成功响应")
        void takePhoto_ShouldReturnSuccessResult() {
            when(physicsService.triggerCamera("SAT-001")).thenReturn("IMG-1234567890");

            Result<String> result = satController.takePhoto("SAT-001");

            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
            assertTrue(result.getData().contains("IMG-"));
            verify(physicsService).triggerCamera("SAT-001");
        }

        @Test
        @DisplayName("拍照失败应抛出业务异常")
        void takePhoto_WhenSatelliteNotFound_ShouldThrowException() {
            when(physicsService.triggerCamera("SAT-001")).thenReturn("卫星不存在");

            assertThrows(Exception.class, () -> satController.takePhoto("SAT-001"));
        }

        @Test
        @DisplayName("电量不足应抛出业务异常")
        void takePhoto_WhenLowBattery_ShouldThrowException() {
            when(physicsService.triggerCamera("SAT-001")).thenReturn("电量不足，请充电");

            assertThrows(Exception.class, () -> satController.takePhoto("SAT-001"));
        }
    }

    @Nested
    @DisplayName("聚焦模式测试")
    class FocusTests {

        @Test
        @DisplayName("设置聚焦应返回成功响应")
        void setFocus_ShouldReturnSuccessResult() {
            when(physicsService.setFocusTarget("SAT-001")).thenReturn("Focus set to SAT-001");

            Result<String> result = satController.setFocus("SAT-001");

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("SAT-001") || result.getData().contains("Focus"));
            verify(physicsService).setFocusTarget("SAT-001");
        }

        @Test
        @DisplayName("卫星不存在应抛出异常")
        void setFocus_WhenSatelliteNotFound_ShouldThrowException() {
            when(physicsService.setFocusTarget("SAT-001")).thenReturn("卫星不存在");

            assertThrows(Exception.class, () -> satController.setFocus("SAT-001"));
        }
    }

    @Nested
    @DisplayName("指向模式测试")
    class PointingTests {

        @Test
        @DisplayName("设置指向模式为太阳应返回成功响应")
        void setPointing_ToSun_ShouldReturnSuccess() {
            Result<String> result = satController.setPointing("SAT-001", "sun");

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("sun"));
            verify(physicsService).setPointingMode("SAT-001", "sun");
        }

        @Test
        @DisplayName("设置指向模式为地球应返回成功响应")
        void setPointing_ToEarth_ShouldReturnSuccess() {
            Result<String> result = satController.setPointing("SAT-001", "earth");

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("earth"));
            verify(physicsService).setPointingMode("SAT-001", "earth");
        }

        @Test
        @DisplayName("设置指向模式为自由应返回成功响应")
        void setPointing_ToFree_ShouldReturnSuccess() {
            Result<String> result = satController.setPointing("SAT-001", "free");

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("free"));
            verify(physicsService).setPointingMode("SAT-001", "free");
        }
    }

    @Nested
    @DisplayName("热控模式测试")
    class ThermalTests {

        @Test
        @DisplayName("设置热控模式为加热器应返回成功响应")
        void setThermal_ToHeater_ShouldReturnSuccess() {
            Result<String> result = satController.setThermal("SAT-001", "heater");

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("heater"));
            verify(physicsService).setThermalMode("SAT-001", "heater");
        }

        @Test
        @DisplayName("设置热控模式为制冷器应返回成功响应")
        void setThermal_ToCooler_ShouldReturnSuccess() {
            Result<String> result = satController.setThermal("SAT-001", "cooler");

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("cooler"));
            verify(physicsService).setThermalMode("SAT-001", "cooler");
        }

        @Test
        @DisplayName("设置热控模式为自动应返回成功响应")
        void setThermal_ToAuto_ShouldReturnSuccess() {
            Result<String> result = satController.setThermal("SAT-001", "auto");

            assertEquals(200, result.getCode());
            assertTrue(result.getData().contains("auto"));
            verify(physicsService).setThermalMode("SAT-001", "auto");
        }
    }
}