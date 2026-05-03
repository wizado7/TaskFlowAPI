package com.tasktracker.user.service;

import com.tasktracker.user.dto.UserProfileUpdateRequest;
import com.tasktracker.user.entity.UserProfile;
import com.tasktracker.user.exception.AppException;
import com.tasktracker.user.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserProfileService {

    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final UserProfileRepository repository;
    private final Path avatarDir;

    public UserProfileService(UserProfileRepository repository,
                              @Value("${app.user-avatars.dir:uploads/user-avatars}") String avatarDir) {
        this.repository = repository;
        this.avatarDir = Path.of(avatarDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.avatarDir);
        } catch (IOException exception) {
            throw new AppException("Avatar storage is not accessible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UserProfile getOrCreateByEmail(String email) {
        return getOrCreateByEmail(email, null);
    }

    public UserProfile getOrCreateByEmail(String email, String fullName) {
        return repository.findByUserEmail(email).map(profile -> {
            if ((profile.getFullName() == null || profile.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
                profile.setFullName(fullName);
                return repository.save(profile);
            }
            return profile;
        }).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUserEmail(email);
            if (fullName != null && !fullName.isBlank()) {
                profile.setFullName(fullName);
            }
            return repository.save(profile);
        });
    }

    public UserProfile updateProfile(String email, UserProfileUpdateRequest request) {
        UserProfile profile = repository.findByUserEmail(email)
                .orElseThrow(() -> new AppException("Profile not found", HttpStatus.NOT_FOUND));
        profile.setFullName(request.fullName());
        profile.setPhone(request.phone());
        profile.setTimezone(request.timezone());
        return repository.save(profile);
    }

    public UserProfile uploadAvatar(String email, MultipartFile file) {
        UserProfile profile = repository.findByUserEmail(email)
                .orElseThrow(() -> new AppException("Profile not found", HttpStatus.NOT_FOUND));
        if (file == null || file.isEmpty()) {
            throw new AppException("Avatar file is required", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new AppException("Avatar file size exceeds 5 MB", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new AppException("Avatar file type is not allowed", HttpStatus.BAD_REQUEST);
        }
        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        String fileName = UUID.randomUUID() + extension;
        try {
            Files.copy(file.getInputStream(), avatarDir.resolve(fileName).normalize(), StandardCopyOption.REPLACE_EXISTING);
            if (profile.getAvatarFileName() != null) {
                Files.deleteIfExists(avatarDir.resolve(profile.getAvatarFileName()).normalize());
            }
        } catch (IOException exception) {
            throw new AppException("Failed to store avatar", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        profile.setAvatarFileName(fileName);
        profile.setAvatarContentType(contentType);
        return repository.save(profile);
    }

    public AvatarDownload downloadAvatar(UUID profileId) {
        UserProfile profile = repository.findById(profileId)
                .orElseThrow(() -> new AppException("Profile not found", HttpStatus.NOT_FOUND));
        if (profile.getAvatarFileName() == null || profile.getAvatarFileName().isBlank()) {
            throw new AppException("Avatar not found", HttpStatus.NOT_FOUND);
        }
        Path path = avatarDir.resolve(profile.getAvatarFileName()).normalize();
        if (!Files.exists(path)) {
            throw new AppException("Avatar file is missing", HttpStatus.NOT_FOUND);
        }
        return new AvatarDownload(
                new FileSystemResource(path),
                profile.getAvatarContentType() == null ? "application/octet-stream" : profile.getAvatarContentType()
        );
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

    public List<UserProfile> findByEmails(List<String> emails) {
        return repository.findByUserEmailInIgnoreCase(emails);
    }

    public record AvatarDownload(Resource resource, String contentType) {}
}
