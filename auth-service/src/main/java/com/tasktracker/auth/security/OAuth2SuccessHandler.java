package com.tasktracker.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasktracker.auth.dto.AuthResponse;
import com.tasktracker.auth.entity.AuthProvider;
import com.tasktracker.auth.entity.Role;
import com.tasktracker.auth.entity.UserAccount;
import com.tasktracker.auth.repository.UserAccountRepository;
import com.tasktracker.auth.service.JwtService;
import com.tasktracker.auth.service.RefreshTokenService;
import com.tasktracker.auth.service.UserProfileProvisioner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserAccountRepository repository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserProfileProvisioner profileProvisioner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuth2SuccessHandler(UserAccountRepository repository,
                                JwtService jwtService,
                                RefreshTokenService refreshTokenService,
                                UserProfileProvisioner profileProvisioner) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.profileProvisioner = profileProvisioner;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth2 provider");
            return;
        }

        UserAccount user = repository.findByEmail(email).orElseGet(() -> {
            UserAccount newUser = new UserAccount();
            newUser.setEmail(email);
            newUser.setProvider(AuthProvider.GOOGLE);
            newUser.getRoles().add(Role.USER);
            return repository.save(newUser);
        });

        profileProvisioner.provision(user.getEmail());

        AuthResponse authResponse = refreshTokenService.issueTokens(user);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), authResponse);
    }
}
