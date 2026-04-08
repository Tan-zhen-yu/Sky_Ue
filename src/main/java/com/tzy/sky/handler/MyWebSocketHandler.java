package com.tzy.sky.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzy.sky.dto.payload.TimeScalePayload;
import com.tzy.sky.dto.protocol.WsAction;
import com.tzy.sky.dto.protocol.WsTarget;
import com.tzy.sky.service.PhysicsService;
import com.tzy.sky.utils.SpringContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    private static final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String jsonStr = message.getPayload();
        JsonNode root = mapper.readTree(jsonStr);

        if (!root.has("target") || !root.has("action")) return;

        String target = root.get("target").asText();
        String action = root.get("action").asText();
        JsonNode payload = root.get("payload");

        // === 核心修复：Java 必须处理控制指令 ===
        PhysicsService service = SpringContextHolder.getBean(PhysicsService.class);

        // 1. 处理系统指令 (时间流速)
        if ("sys".equals(target)) {
            if ("timescale".equals(action)) {
                service.setTimeScale(payload.get("scale").asDouble());
            }

        }
        else if ("sat".equals(target)) {
            if ("focus_mode".equals(action)) {
                String satId = payload.get("id").asText();
                log.info("收到WebSocket聚焦请求: {}", satId);
            }
            if (payload.has("id")) {
                String id = payload.get("id").asText();

                if ("thermal_ctrl".equals(action)) {
                    service.setThermalMode(id, payload.get("mode").asText());
                }
                else if ("point_to".equals(action)) {
                    service.setPointingMode(id, payload.get("target").asText());
                }
                else if ("adjust_attitude".equals(action)) {
                    service.setAttitude(id, payload.get("axis").asText(), payload.get("delta").asDouble());
                }
            }
        }

        // 2. 处理卫星控制指令 (姿态、热控、指向)
        else if ("server".equals(target) && "attitude_report".equals(action)) {
            String id = payload.get("id").asText();
            JsonNode rot = payload.get("rot");

            // 获取 P, Y, R
            double p = rot.get("p").asDouble();
            double y = rot.get("y").asDouble();
            double r = rot.get("r").asDouble();

            // 更新你的 Service 内存中的卫星状态
            service.updateRealRotation(id, p, y, r);
        }

        // 3. 处理传感器回传 (Server)
        else if ("server".equals(target)) {
            if ("sensor_report".equals(action)) {
                service.updateSensorStatus(payload.get("id").asText(), payload.get("occluded").asBoolean());
            }
        }

        // 4. 最后：将指令广播给 UE5 (让UE去执行视觉表现)
        // 注意：不要广播给自己(前端)，否则可能会造成回环，但为了保险起见广播给所有非己方
        for (WebSocketSession client : sessions) {
            // 不要发给自己，且必须加锁
            if (client.isOpen() && !client.getId().equals(session.getId())) {
                synchronized (client) { // <--- 必须加锁！
                    client.sendMessage(message);
                }
            }
        }
    }

    public void broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession client : sessions) {
            if (client.isOpen()) {
                try {
                    synchronized (client) { // <--- 必须加锁！
                        client.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}