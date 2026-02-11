package com.tasktracker.task.dto;

import java.util.UUID;

public record BoardColumnResponse(
        UUID id,
        UUID boardId,
        String name,
        int position
) {}
