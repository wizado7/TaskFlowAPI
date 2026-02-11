package com.tasktracker.user.controller;

import com.tasktracker.user.dto.UserProfileResponse;
import com.tasktracker.user.dto.UserProvisionRequest;
import com.tasktracker.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final UserProfileService service;

    public InternalUserController(UserProfileService service) {
        this.service = service;
    }

    @PostMapping("/provision")
    public ResponseEntity<UserProfileResponse> provision(@Valid @RequestBody UserProvisionRequest request) {
        var profile = service.getOrCreateByEmail(request.email());
        return ResponseEntity.ok(new UserProfileResponse(
                profile.getId(),
                profile.getUserEmail(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getTimezone(),
                profile.getAvatarUrl()
        ));
    }
}
