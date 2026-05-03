package com.tasktracker.task.dto;

import java.util.UUID;

public record SprintCompleteRequest(
        UUID targetBoardId
) {}
