package com.tasktracker.auth.controller;

import com.tasktracker.auth.dto.AuthResponse;
import com.tasktracker.auth.dto.LoginRequest;
import com.tasktracker.auth.dto.RefreshTokenRequest;
import com.tasktracker.auth.dto.RegisterRequest;
import com.tasktracker.auth.service.JwtService;
import com.tasktracker.auth.service.RefreshTokenService;
import com.tasktracker.auth.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserAccountService userAccountService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserAccountService userAccountService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService) {
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        var user = userAccountService.register(request);
        return ResponseEntity.ok(refreshTokenService.issueTokens(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        userAccountService.authenticate(request);
        var user = userAccountService.getByEmail(request.email());
        return ResponseEntity.ok(refreshTokenService.issueTokens(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenService.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<String> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(jwt.getSubject());
    }
}
