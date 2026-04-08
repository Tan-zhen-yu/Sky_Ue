package com.tzy.sky.config;

import com.tzy.sky.handler.MyWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 核心地址： ws://localhost:8080/ws/socket
        registry.addHandler(new MyWebSocketHandler(), "/ws/socket")
                .setAllowedOrigins("*"); // 允许跨域（这句很重要，否则UE可能连不上）
    }
}