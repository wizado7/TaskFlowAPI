package com.tasktracker.task.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TaskEstimateRequest(
        @Min(0) @Max(100) Integer storyPoints
) {}
