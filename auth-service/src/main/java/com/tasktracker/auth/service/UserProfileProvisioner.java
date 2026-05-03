package com.tasktracker.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class UserProfileProvisioner {

    private static final Logger log = LoggerFactory.getLogger(UserProfileProvisioner.class);

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserProfileProvisioner(RestTemplate restTemplate,
                                  @Value("${app.user-service.url:http://user-service:8082}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    public void provision(String email) {
        provision(email, null);
    }

    public void provision(String email, String fullName) {
        try {
            String url = userServiceUrl + "/api/v1/internal/users/provision";
            Map<String, String> body = fullName == null || fullName.isBlank()
                    ? Map.of("email", email)
                    : Map.of("email", email, "fullName", fullName);
            restTemplate.postForEntity(url, body, Void.class);
        } catch (Exception ex) {
            log.warn("Failed to provision user profile for {}: {}", email, ex.getMessage());
        }
    }
}
