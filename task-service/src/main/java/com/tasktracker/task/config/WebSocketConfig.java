package com.tasktracker.task.config;

import com.tasktracker.task.realtime.TaskRealtimeWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TaskRealtimeWebSocketHandler handler;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(TaskRealtimeWebSocketHandler handler,
                           @Value("${app.websocket.allowed-origin-patterns:http://localhost:3000,http://localhost:5173}") String allowedOriginPatterns) {
        this.handler = handler;
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/task-events")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}
