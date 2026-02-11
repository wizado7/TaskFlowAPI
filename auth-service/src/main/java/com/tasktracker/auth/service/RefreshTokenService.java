package com.tasktracker.auth.service;

import com.tasktracker.auth.dto.AuthResponse;
import com.tasktracker.auth.entity.RefreshToken;
import com.tasktracker.auth.entity.UserAccount;
import com.tasktracker.auth.exception.AppException;
import com.tasktracker.auth.repository.RefreshTokenRepository;
import com.tasktracker.auth.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final UserAccountRepository userRepository;
    private final JwtService jwtService;
    private final long refreshExpirationDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               UserAccountRepository userRepository,
                               JwtService jwtService,
                               @Value("${app.jwt.refresh-expiration-days:14}") long refreshExpirationDays) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    public AuthResponse issueTokens(UserAccount user) {
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRoles());
        String refreshToken = createRefreshToken(user);
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                getRefreshExpirationSeconds()
        );
    }

    public AuthResponse refresh(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshToken stored = repository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }
        UserAccount user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.UNAUTHORIZED));
        if (user.isBlocked()) {
            throw new AppException("User blocked", HttpStatus.UNAUTHORIZED);
        }
        stored.setRevoked(true);
        repository.save(stored);
        return issueTokens(user);
    }

    private String createRefreshToken(UserAccount user) {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(hashToken(rawToken));
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS));
        repository.save(token);
        return rawToken;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hashBytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new AppException("Token hash failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private long getRefreshExpirationSeconds() {
        return refreshExpirationDays * 24L * 60L * 60L;
    }
}
