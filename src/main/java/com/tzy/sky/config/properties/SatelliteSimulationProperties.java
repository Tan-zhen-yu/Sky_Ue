package com.tzy.sky.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 卫星仿真系统配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "")
public class SatelliteSimulationProperties {

    /**
     * UE5 渲染引擎配置
     */
    private UE5Config ue5 = new UE5Config();

    /**
     * 物理引擎配置
     */
    private PhysicsConfig physics = new PhysicsConfig();

    /**
     * 地面站配置
     */
    private List<GroundStationConfig> groundStations;

    /**
     * 卫星配置
     */
    private List<SatelliteConfig> satellites;

    @Data
    public static class UE5Config {
        private SignallingConfig signalling = new SignallingConfig();
        private ExecutableConfig executable = new ExecutableConfig();

        @Data
        public static class SignallingConfig {
            private String scriptPath;
            private int startupDelay = 2;
        }

        @Data
        public static class ExecutableConfig {
            private String path;
            private String pixelStreamingUrl;
            private ResolutionConfig resolution = new ResolutionConfig();
            private PerformanceConfig performance = new PerformanceConfig();
            private WindowConfig window = new WindowConfig();
            private String audio;

            @Data
            public static class ResolutionConfig {
                private int width = 1920;
                private int height = 1080;
            }

            @Data
            public static class PerformanceConfig {
                private int screenPercentage = 67;
                private String encoderRateControl = "CBR";
                private int encoderBitrate = 8000000;
                private int encoderMaxQp = 30;
            }

            @Data
            public static class WindowConfig {
                private String mode = "-Windowed";
                private String renderOffScreen = "-RenderOffScreen";
                private int winY = 0;
                private String allowSecondaryDisplays = "-AllowSecondaryDisplays";
            }
        }
    }

    @Data
    public static class PhysicsConfig {
        private EarthConfig earth = new EarthConfig();
        private SimulationConfig simulation = new SimulationConfig();
        private ConnectionConfig connection = new ConnectionConfig();
        private EnergyConfig energy = new EnergyConfig();
        private PropulsionConfig propulsion = new PropulsionConfig();
        private ThermalConfig thermal = new ThermalConfig();

        @Data
        public static class EarthConfig {
            private double radiusKm = 6371.0;
            private double gm = 398600.0;
        }

        @Data
        public static class SimulationConfig {
            private int tickRateHz = 20;
            private double timeScale = 1.0;
        }

        @Data
        public static class ConnectionConfig {
            private double lowOrbitThreshold = 10000.0;
            private double lowOrbitDistance = 6000.0;
            private double highOrbitDistance = 50000.0;
        }

        @Data
        public static class EnergyConfig {
            private double baseDrainRate = 0.05;
            private double solarChargeRate = 0.5;
            private double thermalDrainRate = 0.3;
            private double photoBatteryThreshold = 10.0;
        }

        @Data
        public static class PropulsionConfig {
            private double engineWarmupTime = 3.0;
            private double fuelConsumptionRate = 2.0;
            private double orbitChangeRate = 10.0;
        }

        @Data
        public static class ThermalConfig {
            private double shadowTemperature = -100.0;
            private double sunTemperature = 150.0;
            private double engineWarmupHeat = 50.0;
            private double engineActiveHeat = 300.0;
            private double heaterTarget = 200.0;
            private double coolerTarget = -50.0;
            private double temperatureChangeRate = 0.1;
        }
    }

    @Data
    public static class GroundStationConfig {
        private String name;
        private double x;
        private double y;
        private double z;
    }

    @Data
    public static class SatelliteConfig {
        private String id;
        private double orbitRadius;
        private double startPhase;
        private double inclination;
    }
}
