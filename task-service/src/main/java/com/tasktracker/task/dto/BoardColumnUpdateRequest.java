package com.tasktracker.task.dto;

public record BoardColumnUpdateRequest(
        String name,
        Integer position
) {}
