package com.tasktracker.auth.controller;

import com.tasktracker.auth.dto.AdminCreateRequest;
import com.tasktracker.auth.dto.RoleUpdateRequest;
import com.tasktracker.auth.dto.UserStatusResponse;
import com.tasktracker.auth.entity.UserAccount;
import com.tasktracker.auth.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserAccountService userAccountService;

    public AdminController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserStatusResponse> createAdmin(@Valid @RequestBody AdminCreateRequest request) {
        UserAccount user = userAccountService.createAdmin(request);
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<UserStatusResponse> updateRoles(@PathVariable UUID id,
                                                          @Valid @RequestBody RoleUpdateRequest request) {
        UserAccount user = userAccountService.updateRoles(id, request.roles());
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<UserStatusResponse> blockUser(@PathVariable UUID id) {
        UserAccount user = userAccountService.blockUser(id, true);
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/users/{id}/unblock")
    public ResponseEntity<UserStatusResponse> unblockUser(@PathVariable UUID id) {
        UserAccount user = userAccountService.blockUser(id, false);
        return ResponseEntity.ok(toResponse(user));
    }

    private UserStatusResponse toResponse(UserAccount user) {
        return new UserStatusResponse(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()),
                user.isBlocked()
        );
    }
}
