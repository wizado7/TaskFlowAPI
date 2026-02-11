package com.tasktracker.user.service;

import com.tasktracker.user.dto.UserProfileUpdateRequest;
import com.tasktracker.user.entity.UserProfile;
import com.tasktracker.user.exception.AppException;
import com.tasktracker.user.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public UserProfile getOrCreateByEmail(String email) {
        return repository.findByUserEmail(email).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUserEmail(email);
            return repository.save(profile);
        });
    }

    public UserProfile updateProfile(String email, UserProfileUpdateRequest request) {
        UserProfile profile = repository.findByUserEmail(email)
                .orElseThrow(() -> new AppException("Profile not found", HttpStatus.NOT_FOUND));
        profile.setFullName(request.fullName());
        profile.setPhone(request.phone());
        profile.setTimezone(request.timezone());
        profile.setAvatarUrl(request.avatarUrl());
        return repository.save(profile);
    }

    public List<UserProfile> listAll() {
        return repository.findAll();
    }

    public List<UserProfile> searchByEmail(String email) {
        return repository.findByUserEmailContainingIgnoreCase(email);
    }

    public List<UserProfile> searchByPhone(String phone) {
        return repository.findByPhoneContainingIgnoreCase(phone);
    }

    public List<UserProfile> searchByName(String name) {
        return repository.findByFullNameContainingIgnoreCase(name);
    }
}
