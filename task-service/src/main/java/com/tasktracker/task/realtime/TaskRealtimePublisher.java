package com.tasktracker.task.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskRealtimePublisher {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> boardSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> projectSessions = new ConcurrentHashMap<>();

    public TaskRealtimePublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void register(WebSocketSession session, UUID projectId, UUID boardId) {
        if (projectId != null) {
            projectSessions.computeIfAbsent(projectId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        }
        if (boardId != null) {
            boardSessions.computeIfAbsent(boardId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        }
    }

    void unregister(WebSocketSession session) {
        projectSessions.values().forEach(sessions -> sessions.remove(session));
        boardSessions.values().forEach(sessions -> sessions.remove(session));
    }

    public void publish(TaskRealtimeEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            return;
        }

        if (event.projectId() != null) {
            send(projectSessions.get(event.projectId()), payload);
        }
        if (event.boardId() != null) {
            send(boardSessions.get(event.boardId()), payload);
        }
    }

    private void send(Set<WebSocketSession> sessions, String payload) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(message);
            } catch (IOException exception) {
                sessions.remove(session);
            }
        }
    }
}
