package com.tasktracker.task.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NotificationClient(RestTemplate restTemplate,
                              @Value("${USER_SERVICE_URL:http://user-service:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void sendProjectInvite(String email, String projectName, String token, String inviterEmail) {
        var request = new NotificationCreateRequest(
                email,
            "Project invite",
            "You were invited to project \"" + projectName + "\" by " + inviterEmail,
                "PROJECT_INVITE",
                "/api/v1/projects/invites/" + token + "/accept",
            "Accept"
        );
        restTemplate.postForEntity(baseUrl + "/api/v1/internal/notifications", request, Void.class);
    }

    private record NotificationCreateRequest(
            String email,
            String title,
            String message,
            String type,
            String actionUrl,
            String actionLabel
    ) {}
}
