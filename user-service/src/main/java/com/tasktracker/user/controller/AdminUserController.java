package com.tasktracker.user.controller;

import com.tasktracker.user.dto.UserProfileResponse;
import com.tasktracker.user.entity.UserProfile;
import com.tasktracker.user.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserProfileService service;

    public AdminUserController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> list() {
        var users = service.listAll().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserProfileResponse>> search(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false, name = "name") String name
    ) {
        List<UserProfile> profiles;
        if (email != null && !email.isBlank()) {
            profiles = service.searchByEmail(email);
        } else if (phone != null && !phone.isBlank()) {
            profiles = service.searchByPhone(phone);
        } else if (name != null && !name.isBlank()) {
            profiles = service.searchByName(name);
        } else {
            profiles = service.listAll();
        }
        return ResponseEntity.ok(profiles.stream().map(this::toResponse).toList());
    }

    private UserProfileResponse toResponse(UserProfile profile) {
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
