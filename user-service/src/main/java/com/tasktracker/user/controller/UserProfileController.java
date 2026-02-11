package com.tasktracker.user.controller;

import com.tasktracker.user.dto.UserProfileResponse;
import com.tasktracker.user.dto.UserProfileUpdateRequest;
import com.tasktracker.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal Jwt jwt) {
        var profile = service.getOrCreateByEmail(jwt.getSubject());
        return ResponseEntity.ok(toResponse(profile));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> update(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody UserProfileUpdateRequest request) {
        var profile = service.updateProfile(jwt.getSubject(), request);
        return ResponseEntity.ok(toResponse(profile));
    }

    private UserProfileResponse toResponse(com.tasktracker.user.entity.UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUserEmail(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getTimezone(),
                profile.getAvatarUrl()
        );
    }
}
