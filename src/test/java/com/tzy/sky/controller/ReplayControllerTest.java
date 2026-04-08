package com.tzy.sky.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzy.sky.dto.ReplayQueryDTO;
import com.tzy.sky.dto.ReplayResponseDTO;
import com.tzy.sky.dto.StreamRequestDTO;
import com.tzy.sky.service.ReplayService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("回放控制器单元测试")
class ReplayControllerTest {

    @Mock
    private ReplayService replayService;

    @InjectMocks
    private ReplayController replayController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("批量查询回放数据测试")
    class QueryReplayDataTests {

        @Test
        @DisplayName("查询有效请求应返回200状态码")
        void queryReplayData_WithValidRequest_ShouldReturn200() {
            ReplayQueryDTO queryDTO = new ReplayQueryDTO();
            queryDTO.setSatelliteId("SAT-001");
            queryDTO.setStartTime(LocalDateTime.now().minusHours(1));
            queryDTO.setEndTime(LocalDateTime.now());

            ReplayResponseDTO mockResponse = new ReplayResponseDTO();
            mockResponse.setSatelliteId("SAT-001");
            mockResponse.setData(new ArrayList<>());
            mockResponse.setReturnedCount(0L);

            when(replayService.queryReplayData(any(ReplayQueryDTO.class))).thenReturn(mockResponse);

            ResponseEntity<ReplayResponseDTO> response = replayController.queryReplayData(queryDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("SAT-001", response.getBody().getSatelliteId());
        }

        @Test
        @DisplayName("卫星ID为空应返回400状态码")
        void queryReplayData_WithNullSatelliteId_ShouldReturn400() {
            ReplayQueryDTO queryDTO = new ReplayQueryDTO();
            queryDTO.setSatelliteId(null);

            ResponseEntity<ReplayResponseDTO> response = replayController.queryReplayData(queryDTO);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("无时间范围应自动设置为最近24小时")
        void queryReplayData_WithNoTimeRange_ShouldSetDefaultRange() {
            ReplayQueryDTO queryDTO = new ReplayQueryDTO();
            queryDTO.setSatelliteId("SAT-001");

            ReplayResponseDTO mockResponse = new ReplayResponseDTO();
            mockResponse.setSatelliteId("SAT-001");
            mockResponse.setData(new ArrayList<>());
            mockResponse.setReturnedCount(0L);

            when(replayService.queryReplayData(any(ReplayQueryDTO.class))).thenReturn(mockResponse);

            replayController.queryReplayData(queryDTO);

            assertNotNull(queryDTO.getStartTime());
            assertNotNull(queryDTO.getEndTime());
        }
    }

    @Nested
    @DisplayName("快速查询测试")
    class QuickQueryTests {

        @Test
        @DisplayName("快速查询应返回正确数据")
        void quickQuery_ShouldReturnCorrectData() {
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();

            ReplayResponseDTO mockResponse = new ReplayResponseDTO();
            mockResponse.setSatelliteId("SAT-001");
            mockResponse.setData(new ArrayList<>());
            mockResponse.setReturnedCount(0L);

            when(replayService.queryReplayData(any(ReplayQueryDTO.class))).thenReturn(mockResponse);

            ResponseEntity<ReplayResponseDTO> response = replayController.quickQuery(
                    "SAT-001", startTime, endTime, null, 10000
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("指定字段查询应正确传递")
        void quickQuery_WithFields_ShouldPassFields() {
            LocalDateTime startTime = LocalDateTime.now().minusHours(1);
            LocalDateTime endTime = LocalDateTime.now();
            List<String> fields = Arrays.asList("position", "battery");

            ReplayResponseDTO mockResponse = new ReplayResponseDTO();
            mockResponse.setSatelliteId("SAT-001");
            mockResponse.setData(new ArrayList<>());
            mockResponse.setReturnedCount(0L);

            when(replayService.queryReplayData(any(ReplayQueryDTO.class))).thenReturn(mockResponse);

            ResponseEntity<ReplayResponseDTO> response = replayController.quickQuery(
                    "SAT-001", startTime, endTime, fields, 10000
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("流式回放测试")
    class StreamReplayTests {

        @Test
        @DisplayName("启动流式回放应返回SseEmitter")
        void startStreamReplay_ShouldReturnSseEmitter() {
            when(replayService.startStreamReplay(any(StreamRequestDTO.class)))
                    .thenReturn(new SseEmitter());

            SseEmitter emitter = replayController.startStreamReplay(
                    "SAT-001", 0.0, 1.0, 60
            );

            assertNotNull(emitter);
            verify(replayService).startStreamReplay(any(StreamRequestDTO.class));
        }

        @Test
        @DisplayName("启动流式回放应正确设置参数")
        void startStreamReplay_ShouldSetCorrectParams() {
            SseEmitter mockEmitter = new SseEmitter();
            when(replayService.startStreamReplay(any(StreamRequestDTO.class)))
                    .thenReturn(mockEmitter);

            SseEmitter emitter = replayController.startStreamReplay("SAT-001", 100.0, 2.0, 120);

            assertNotNull(emitter);
            verify(replayService).startStreamReplay(any(StreamRequestDTO.class));
        }

        @Test
        @DisplayName("停止流式回放应返回正确响应")
        void stopStream_ShouldReturnCorrectResponse() {
            ResponseEntity<Map<String, Object>> response = replayController.stopStream("test-stream-id");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("test-stream-id", response.getBody().get("streamId"));
            assertEquals("stopped", response.getBody().get("status"));
            assertNotNull(response.getBody().get("timestamp"));
        }
    }

    @Nested
    @DisplayName("卫星列表测试")
    class SatellitesTests {

        @Test
        @DisplayName("获取卫星列表应返回200状态码")
        void getSatellites_ShouldReturn200() {
            List<String> mockSatellites = Arrays.asList("SAT-001", "SAT-002", "SAT-003");
            when(replayService.getAvailableSatellites()).thenReturn(mockSatellites);

            ResponseEntity<List<String>> response = replayController.getSatellites();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(3, response.getBody().size());
        }
    }

    @Nested
    @DisplayName("时间范围查询测试")
    class TimeRangeTests {

        @Test
        @DisplayName("获取时间范围应返回正确数据")
        void getTimeRange_ShouldReturnCorrectData() {
            Map<String, Object> mockRange = new HashMap<>();
            mockRange.put("startTime", System.currentTimeMillis() - 3600000);
            mockRange.put("endTime", System.currentTimeMillis());

            when(replayService.getTimeRange("SAT-001")).thenReturn(mockRange);

            ResponseEntity<Map<String, Object>> response = replayController.getTimeRange("SAT-001");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().get("dataSource"));
        }
    }

    @Nested
    @DisplayName("数据统计测试")
    class StatisticsTests {

        @Test
        @DisplayName("获取统计数据应返回正确结构")
        void getStatistics_ShouldReturnCorrectStructure() {
            Map<String, Object> mockStats = new HashMap<>();
            mockStats.put("satelliteId", "SAT-001");
            mockStats.put("totalRecords", 3600L);

            when(replayService.getStatistics("SAT-001")).thenReturn(mockStats);

            ResponseEntity<Map<String, Object>> response = replayController.getStatistics("SAT-001");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("SAT-001", response.getBody().get("satelliteId"));
        }
    }

    @Nested
    @DisplayName("健康检查测试")
    class HealthTests {

        @Test
        @DisplayName("健康检查应返回UP状态")
        void health_ShouldReturnUpStatus() {
            ResponseEntity<Map<String, Object>> response = replayController.health();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("UP", response.getBody().get("status"));
            assertEquals("replay-service", response.getBody().get("service"));
            assertEquals("satellite_data_sampled (1Hz)", response.getBody().get("dataSource"));
            assertNotNull(response.getBody().get("timestamp"));
        }
    }
}