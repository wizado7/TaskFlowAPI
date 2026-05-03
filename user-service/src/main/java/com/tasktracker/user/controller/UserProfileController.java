package com.tasktracker.user.controller;

import com.tasktracker.user.dto.UserProfileResponse;
import com.tasktracker.user.dto.UserProfileUpdateRequest;
import com.tasktracker.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadAvatar(@AuthenticationPrincipal Jwt jwt,
                                                            @RequestParam("file") MultipartFile file) {
        var profile = service.uploadAvatar(jwt.getSubject(), file);
        return ResponseEntity.ok(toResponse(profile));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<Resource> avatar(@PathVariable("id") UUID id) {
        var download = service.downloadAvatar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.resource());
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<UserProfileResponse>> lookup(@RequestParam("emails") String emails) {
        List<String> values = Arrays.stream(emails.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();
        return ResponseEntity.ok(service.findByEmails(values).stream().map(this::toResponse).toList());
    }

    private UserProfileResponse toResponse(com.tasktracker.user.entity.UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUserEmail(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getTimezone(),
                profile.getAvatarFileName() == null ? null : "/api/v1/users/" + profile.getId() + "/avatar"
        );
    }
}
