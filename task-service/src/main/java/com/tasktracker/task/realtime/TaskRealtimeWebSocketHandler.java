package com.tasktracker.task.realtime;

import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.service.BoardAccessService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class TaskRealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final String ACCESS_TOKEN_COOKIE = "tf_access_token";

    private final JwtDecoder jwtDecoder;
    private final BoardAccessService accessService;
    private final TaskRealtimePublisher publisher;

    public TaskRealtimeWebSocketHandler(JwtDecoder jwtDecoder,
                                        BoardAccessService accessService,
                                        TaskRealtimePublisher publisher) {
        this.jwtDecoder = jwtDecoder;
        this.accessService = accessService;
        this.publisher = publisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            URI uri = session.getUri();
            if (uri == null) {
                session.close(CloseStatus.BAD_DATA);
                return;
            }
            MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
            String token = resolveToken(session);
            UUID projectId = parseUuid(params.getFirst("projectId"));
            UUID boardId = parseUuid(params.getFirst("boardId"));
            if (token == null || token.isBlank() || (projectId == null && boardId == null)) {
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            String userEmail = jwtDecoder.decode(token).getSubject();
            if (boardId != null) {
                accessService.requireBoardAccess(boardId, userEmail);
            }
            if (projectId != null) {
                accessService.requireProjectMember(projectId, userEmail);
            }
            publisher.register(session, projectId, boardId);
        } catch (AppException exception) {
            CloseStatus status = exception.getStatus() == HttpStatus.FORBIDDEN
                    ? CloseStatus.POLICY_VIOLATION
                    : CloseStatus.BAD_DATA;
            session.close(status);
        } catch (Exception exception) {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        publisher.unregister(session);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new AppException("Invalid realtime subscription id", HttpStatus.BAD_REQUEST);
        }
    }

    private String resolveToken(WebSocketSession session) {
        String cookieHeader = session.getHandshakeHeaders().getFirst(HttpHeaders.COOKIE);
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }

        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && ACCESS_TOKEN_COOKIE.equals(parts[0])) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
