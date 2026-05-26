package com.tasktracker.auth.security;

import com.tasktracker.auth.entity.AuthProvider;
import com.tasktracker.auth.entity.Role;
import com.tasktracker.auth.entity.UserAccount;
import com.tasktracker.auth.repository.UserAccountRepository;
import com.tasktracker.auth.service.OAuthLoginCodeService;
import com.tasktracker.auth.service.UserProfileProvisioner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserAccountRepository repository;
    private final OAuthLoginCodeService oAuthLoginCodeService;
    private final UserProfileProvisioner profileProvisioner;
    private final String frontendSuccessUrl;

    public OAuth2SuccessHandler(UserAccountRepository repository,
                                OAuthLoginCodeService oAuthLoginCodeService,
                                UserProfileProvisioner profileProvisioner,
                                @Value("${app.frontend.oauth-success-url:http://localhost:3000/auth/callback}") String frontendSuccessUrl) {
        this.repository = repository;
        this.oAuthLoginCodeService = oAuthLoginCodeService;
        this.profileProvisioner = profileProvisioner;
        this.frontendSuccessUrl = frontendSuccessUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");

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

        profileProvisioner.provision(user.getEmail(), fullName);

        String code = oAuthLoginCodeService.createCode(user);
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendSuccessUrl)
                .queryParam("code", code)
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
