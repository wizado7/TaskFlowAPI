package com.tasktracker.task.dto;

import com.tasktracker.task.entity.BoardMethodology;
import com.tasktracker.task.entity.BoardType;

public record BoardUpdateRequest(
        String name,
        BoardType type,
        BoardMethodology methodology
) {}
