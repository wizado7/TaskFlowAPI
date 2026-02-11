package com.tasktracker.auth.dto;

import com.tasktracker.auth.entity.Role;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RoleUpdateRequest(
        @NotNull Set<Role> roles
) {}
