package com.tzy.sky.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzy.sky.dto.protocol.WsAction;
import com.tzy.sky.dto.protocol.WsTarget;
import com.tzy.sky.service.PhysicsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final PhysicsService physicsService;

    @Autowired
    public MyWebSocketHandler(PhysicsService physicsService) {
        this.physicsService = physicsService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket连接已建立: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket连接已关闭: {}, 状态: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String jsonStr = message.getPayload();
        JsonNode root = mapper.readTree(jsonStr);

        if (!root.has("target") || !root.has("action")) return;

        String target = root.get("target").asText();
        String action = root.get("action").asText();
        JsonNode payload = root.get("payload");

        if ("sys".equals(target)) {
            if ("timescale".equals(action)) {
                physicsService.setTimeScale(payload.get("scale").asDouble());
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
                    physicsService.setThermalMode(id, payload.get("mode").asText());
                }
                else if ("point_to".equals(action)) {
                    physicsService.setPointingMode(id, payload.get("target").asText());
                }
                else if ("adjust_attitude".equals(action)) {
                    physicsService.setAttitude(id, payload.get("axis").asText(), payload.get("delta").asDouble());
                }
            }
        }
        else if ("server".equals(target) && "attitude_report".equals(action)) {
            String id = payload.get("id").asText();
            JsonNode rot = payload.get("rot");

            double p = rot.get("p").asDouble();
            double y = rot.get("y").asDouble();
            double r = rot.get("r").asDouble();

            physicsService.updateRealRotation(id, p, y, r);
        }
        else if ("server".equals(target)) {
            if ("sensor_report".equals(action)) {
                physicsService.updateSensorStatus(payload.get("id").asText(), payload.get("occluded").asBoolean());
            }
        }

        for (WebSocketSession client : sessions) {
            if (client.isOpen() && !client.getId().equals(session.getId())) {
                synchronized (client) {
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
                    synchronized (client) {
                        client.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.error("WebSocket广播消息失败: {}", client.getId(), e);
                }
            }
        }
    }
}