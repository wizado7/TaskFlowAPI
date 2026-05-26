package com.tasktracker.auth.service;

import com.tasktracker.auth.dto.AuthResponse;
import com.tasktracker.auth.entity.OAuthLoginCode;
import com.tasktracker.auth.entity.UserAccount;
import com.tasktracker.auth.exception.AppException;
import com.tasktracker.auth.repository.OAuthLoginCodeRepository;
import com.tasktracker.auth.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class OAuthLoginCodeService {

    private final OAuthLoginCodeRepository repository;
    private final UserAccountRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final long codeTtlSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthLoginCodeService(OAuthLoginCodeRepository repository,
                                 UserAccountRepository userRepository,
                                 RefreshTokenService refreshTokenService,
                                 @Value("${app.oauth.login-code-ttl-seconds:300}") long codeTtlSeconds) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.codeTtlSeconds = codeTtlSeconds;
    }

    @Transactional
    public String createCode(UserAccount user) {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        String rawCode = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant now = Instant.now();
        repository.deleteByExpiresAtBefore(now);

        OAuthLoginCode code = new OAuthLoginCode();
        code.setUserId(user.getId());
        code.setCodeHash(hash(rawCode));
        code.setCreatedAt(now);
        code.setExpiresAt(now.plusSeconds(codeTtlSeconds));
        repository.save(code);

        return rawCode;
    }

    @Transactional
    public long cleanupExpiredCodes(Instant now) {
        return repository.deleteByExpiresAtBefore(now);
    }

    @Transactional
    public AuthResponse exchange(String rawCode) {
        OAuthLoginCode code = repository.findByCodeHashForUpdate(hash(rawCode))
                .orElseThrow(() -> new AppException("Invalid OAuth code", HttpStatus.UNAUTHORIZED));
        Instant now = Instant.now();
        if (code.getConsumedAt() != null || code.getExpiresAt().isBefore(now)) {
            throw new AppException("OAuth code expired", HttpStatus.UNAUTHORIZED);
        }

        UserAccount user = userRepository.findById(code.getUserId())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.UNAUTHORIZED));
        if (user.isBlocked()) {
            throw new AppException("User blocked", HttpStatus.UNAUTHORIZED);
        }

        code.setConsumedAt(now);
        repository.save(code);
        return refreshTokenService.issueTokens(user);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new AppException("OAuth code hash failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
