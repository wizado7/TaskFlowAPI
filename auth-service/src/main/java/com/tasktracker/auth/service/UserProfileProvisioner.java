package com.tasktracker.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class UserProfileProvisioner {

    private static final Logger log = LoggerFactory.getLogger(UserProfileProvisioner.class);

    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private final String internalToken;

    public UserProfileProvisioner(RestTemplate restTemplate,
                                  @Value("${app.user-service.url:http://user-service:8082}") String userServiceUrl,
                                  @Value("${app.internal-api.token:}") String internalToken) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        this.internalToken = internalToken;
    }

    public void provision(String email) {
        provision(email, null);
    }

    public void provision(String email, String fullName) {
        tryProvision(email, fullName);
    }

    public void provisionWithRetry(String email, String fullName) {
        int maxAttempts = 8;
        Duration delay = Duration.ofSeconds(2);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (tryProvision(email, fullName)) {
                return;
            }
            if (attempt < maxAttempts) {
                sleep(delay);
            }
        }
    }

    private boolean tryProvision(String email, String fullName) {
        try {
            String url = userServiceUrl + "/api/v1/internal/users/provision";
            Map<String, String> body = fullName == null || fullName.isBlank()
                    ? Map.of("email", email)
                    : Map.of("email", email, "fullName", fullName);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", internalToken);
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to provision user profile for {}: {}", email, ex.getMessage());
            return false;
        }
    }

    private void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
