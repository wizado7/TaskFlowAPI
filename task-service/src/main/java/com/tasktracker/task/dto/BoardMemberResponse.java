package com.tasktracker.task.dto;

import com.tasktracker.task.entity.BoardRole;

import java.util.UUID;

public record BoardMemberResponse(
        UUID id,
        UUID boardId,
        String userEmail,
        BoardRole role
) {}