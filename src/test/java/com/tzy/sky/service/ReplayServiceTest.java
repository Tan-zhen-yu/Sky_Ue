package com.tzy.sky.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tzy.sky.dto.ReplayQueryDTO;
import com.tzy.sky.dto.ReplayResponseDTO;
import com.tzy.sky.dto.StreamRequestDTO;
import com.tzy.sky.entity.SatelliteDataSampled;
import com.tzy.sky.mapper.SatelliteDataSampledMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("回放服务单元测试")
class ReplayServiceTest {

    @Mock
    private SatelliteDataSampledMapper sampledMapper;

    private ReplayService replayService;

    @BeforeEach
    void setUp() {
        replayService = new ReplayService();
        ReflectionTestUtils.setField(replayService, "sampledMapper", sampledMapper);
    }

    @Nested
    @DisplayName("查询回放数据测试")
    class QueryReplayDataTests {

        @Test
        @DisplayName("查询有效时间范围应返回数据")
        void queryReplayData_WithValidTimeRange_ShouldReturnData() {
            ReplayQueryDTO queryDTO = new ReplayQueryDTO();
            queryDTO.setSatelliteId("SAT-001");
            queryDTO.setStartTime(LocalDateTime.now().minusHours(1));
            queryDTO.setEndTime(LocalDateTime.now());

            List<SatelliteDataSampled> mockData = Arrays.asList(
                    createMockSampledData("SAT-001", 1),
                    createMockSampledData("SAT-001", 2)
            );

            when(sampledMapper.selectList(any(QueryWrapper.class))).thenReturn(mockData);

            ReplayResponseDTO response = replayService.queryReplayData(queryDTO);

            assertNotNull(response);
            assertEquals("SAT-001", response.getSatelliteId());
            assertEquals(2, response.getReturnedCount());
            assertNotNull(response.getData());
        }

        @Test
        @DisplayName("查询无数据时应返回空列表")
        void queryReplayData_WithNoData_ShouldReturnEmptyList() {
            ReplayQueryDTO queryDTO = new ReplayQueryDTO();
            queryDTO.setSatelliteId("SAT-001");
            queryDTO.setStartTime(LocalDateTime.now().minusHours(1));
            queryDTO.setEndTime(LocalDateTime.now());

            when(sampledMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

            ReplayResponseDTO response = replayService.queryReplayData(queryDTO);

            assertNotNull(response);
            assertEquals(0, response.getReturnedCount());
            assertTrue(response.getData().isEmpty());
        }

        @Test
        @DisplayName("查询结果应限制返回数量")
        void queryReplayData_ShouldLimitResults() {
            ReplayQueryDTO queryDTO = new ReplayQueryDTO();
            queryDTO.setSatelliteId("SAT-001");
            queryDTO.setStartTime(LocalDateTime.now().minusHours(1));
            queryDTO.setEndTime(LocalDateTime.now());
            queryDTO.setLimit(10);

            List<SatelliteDataSampled> mockData = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                mockData.add(createMockSampledData("SAT-001", i));
            }

            when(sampledMapper.selectList(any(QueryWrapper.class))).thenReturn(mockData);

            ReplayResponseDTO response = replayService.queryReplayData(queryDTO);

            assertEquals(10, response.getReturnedCount());
            assertEquals(10, response.getData().size());
        }

        @Test
        @DisplayName("无limit参数时应正常处理")
        void queryReplayData_WithNullLimit_ShouldHandleGracefully() {
            ReplayQueryDTO queryDTO = new ReplayQueryDTO();
            queryDTO.setSatelliteId("SAT-001");
            queryDTO.setStartTime(LocalDateTime.now().minusHours(1));
            queryDTO.setEndTime(LocalDateTime.now());
            queryDTO.setLimit(null);

            List<SatelliteDataSampled> mockData = Arrays.asList(
                    createMockSampledData("SAT-001", 1)
            );
            when(sampledMapper.selectList(any(QueryWrapper.class))).thenReturn(mockData);

            ReplayResponseDTO response = replayService.queryReplayData(queryDTO);

            assertNotNull(response);
            assertEquals(1, response.getReturnedCount());
        }
    }

    @Nested
    @DisplayName("流式回放测试")
    class StreamReplayTests {

        @Test
        @DisplayName("启动流式回放应返回非null的SseEmitter")
        void startStreamReplay_ShouldReturnNonNullEmitter() {
            StreamRequestDTO request = new StreamRequestDTO();
            request.setSatelliteId("SAT-001");
            request.setStartSimulationTime(0.0);
            request.setSpeed(1.0);
            request.setBufferSize(60);

            SseEmitter emitter = replayService.startStreamReplay(request);

            assertNotNull(emitter);
            replayService.stopStream(emitter.toString());
        }

        @Test
        @DisplayName("停止不存在的流应不抛异常")
        void stopStream_NonExistent_ShouldNotThrow() {
            assertDoesNotThrow(() -> replayService.stopStream("non-existent-stream-id"));
        }
    }

    @Nested
    @DisplayName("可用卫星列表测试")
    class AvailableSatellitesTests {

        @Test
        @DisplayName("获取可用卫星列表应返回非空列表")
        void getAvailableSatellites_ShouldReturnList() {
            List<String> satellites = replayService.getAvailableSatellites();

            assertNotNull(satellites);
            assertFalse(satellites.isEmpty());
        }

        @Test
        @DisplayName("可用卫星列表应包含预设卫星")
        void getAvailableSatellites_ShouldContainSatellites() {
            List<String> satellites = replayService.getAvailableSatellites();

            assertTrue(satellites.contains("SAT-001"));
            assertTrue(satellites.contains("SAT-002"));
            assertTrue(satellites.contains("SAT-003"));
        }
    }

    @Nested
    @DisplayName("时间范围查询测试")
    class TimeRangeTests {

        @Test
        @DisplayName("获取存在数据的时间范围应返回结果")
        void getTimeRange_WithExistingData_ShouldReturnRange() {
            Map<String, Object> mockRange = new HashMap<>();
            mockRange.put("startTime", System.currentTimeMillis() - 3600000);
            mockRange.put("endTime", System.currentTimeMillis());

            when(sampledMapper.selectTimeRange("SAT-001")).thenReturn(mockRange);

            Map<String, Object> range = replayService.getTimeRange("SAT-001");

            assertNotNull(range);
            assertNotNull(range.get("startTime"));
            assertNotNull(range.get("endTime"));
        }

        @Test
        @DisplayName("获取无数据的时间范围应返回空结果")
        void getTimeRange_WithNoData_ShouldReturnEmptyRange() {
            when(sampledMapper.selectTimeRange("SAT-001")).thenReturn(null);

            Map<String, Object> range = replayService.getTimeRange("SAT-001");

            assertNotNull(range);
            assertNull(range.get("startTime"));
            assertNull(range.get("endTime"));
        }
    }

    @Nested
    @DisplayName("数据统计测试")
    class StatisticsTests {

        @Test
        @DisplayName("获取数据统计应返回正确结构")
        void getStatistics_ShouldReturnCorrectStructure() {
            when(sampledMapper.selectCount(any(QueryWrapper.class))).thenReturn(3600L);

            Map<String, Object> stats = replayService.getStatistics("SAT-001");

            assertNotNull(stats);
            assertEquals("SAT-001", stats.get("satelliteId"));
            assertEquals(3600L, stats.get("totalRecords"));
            assertNotNull(stats.get("approximateHours"));
            assertEquals("satellite_data_sampled (1Hz)", stats.get("dataSource"));
        }

        @Test
        @DisplayName("统计数据应正确计算小时数")
        void getStatistics_ShouldCalculateHoursCorrectly() {
            when(sampledMapper.selectCount(any(QueryWrapper.class))).thenReturn(7200L);

            Map<String, Object> stats = replayService.getStatistics("SAT-001");

            assertEquals(2.0, stats.get("approximateHours"));
        }

        @Test
        @DisplayName("无数据时统计应返回零")
        void getStatistics_WithNoData_ShouldReturnZero() {
            when(sampledMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

            Map<String, Object> stats = replayService.getStatistics("SAT-001");

            assertEquals(0L, stats.get("totalRecords"));
            assertEquals(0.0, stats.get("approximateHours"));
        }
    }

    private SatelliteDataSampled createMockSampledData(String satelliteId, int index) {
        return SatelliteDataSampled.builder()
                .id("id-" + index)
                .satelliteId(satelliteId)
                .simulationTime((double) index)
                .timestamp(LocalDateTime.now())
                .positionX(7000.0 + index)
                .positionY(0.0)
                .positionZ(0.0)
                .orbitRadius(7000.0)
                .realPitch(0.0)
                .realRoll(0.0)
                .realYaw(0.0)
                .battery(100.0)
                .solarEfficiency(0.8)
                .temperature(20.0)
                .thermalMode("auto")
                .engineActive(false)
                .isOccluded(false)
                .isScanning(false)
                .pointingMode("earth")
                .connectedStation(null)
                .speed(7.5)
                .createdAt(LocalDateTime.now())
                .build();
    }
}