package com.tasktracker.auth.service;

import com.tasktracker.auth.dto.AuthResponse;
import com.tasktracker.auth.entity.RefreshToken;
import com.tasktracker.auth.entity.Role;
import com.tasktracker.auth.entity.UserAccount;
import com.tasktracker.auth.exception.AppException;
import com.tasktracker.auth.repository.RefreshTokenRepository;
import com.tasktracker.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private JwtService jwtService;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, userRepository, jwtService, 14);
    }

    @Test
    void issueTokensStoresOnlyRefreshTokenHash() {
        UserAccount user = user("lead@example.com", false);
        when(jwtService.generateToken(user.getEmail(), user.getRoles())).thenReturn("access-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = service.issueTokens(user);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotEqualTo("access-token");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(tokenCaptor.capture());
        RefreshToken saved = tokenCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getTokenHash()).isEqualTo(sha256(response.refreshToken()));
        assertThat(saved.getTokenHash()).isNotEqualTo(response.refreshToken());
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void refreshRevokesOldTokenAndIssuesNewPair() {
        UserAccount user = user("lead@example.com", false);
        RefreshToken stored = new RefreshToken();
        stored.setUserId(user.getId());
        stored.setTokenHash(sha256("refresh-token"));
        stored.setCreatedAt(Instant.now());
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        when(repository.findByTokenHashForUpdate(sha256("refresh-token"))).thenReturn(Optional.of(stored));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user.getEmail(), user.getRoles())).thenReturn("new-access-token");

        AuthResponse response = service.refresh("refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(stored.isRevoked()).isTrue();
        verify(repository).save(stored);
        verify(repository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshRejectsBlockedUser() {
        UserAccount user = user("blocked@example.com", true);
        RefreshToken stored = new RefreshToken();
        stored.setUserId(user.getId());
        stored.setTokenHash(sha256("refresh-token"));
        stored.setCreatedAt(Instant.now());
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        when(repository.findByTokenHashForUpdate(sha256("refresh-token"))).thenReturn(Optional.of(stored));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.refresh("refresh-token"))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UserAccount user(String email, boolean blocked) {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setBlocked(blocked);
        user.getRoles().addAll(Set.of(Role.USER));
        return user;
    }

    private String sha256(String token) {
        try {
            byte[] hashBytes = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hashBytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
