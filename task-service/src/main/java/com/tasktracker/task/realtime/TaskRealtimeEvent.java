package com.tasktracker.task.realtime;

import java.time.Instant;
import java.util.UUID;

public record TaskRealtimeEvent(
        RealtimeResource resource,
        RealtimeAction action,
        UUID projectId,
        UUID boardId,
        UUID taskId,
        UUID entityId,
        String actorEmail,
        Instant occurredAt
) {
    public static TaskRealtimeEvent of(RealtimeResource resource,
                                       RealtimeAction action,
                                       UUID projectId,
                                       UUID boardId,
                                       UUID taskId,
                                       UUID entityId,
                                       String actorEmail) {
        return new TaskRealtimeEvent(resource, action, projectId, boardId, taskId, entityId, actorEmail, Instant.now());
    }
}
