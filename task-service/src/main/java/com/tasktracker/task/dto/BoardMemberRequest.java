package com.tasktracker.task.dto;

import com.tasktracker.task.entity.BoardRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record BoardMemberRequest(
        @Email @NotBlank String email,
        BoardRole role
) {}