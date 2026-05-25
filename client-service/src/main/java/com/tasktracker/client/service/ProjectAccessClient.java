package com.tasktracker.client.service;

import com.tasktracker.client.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class ProjectAccessClient {

    private final RestTemplate restTemplate;
    private final String taskServiceUrl;

    public ProjectAccessClient(RestTemplate restTemplate,
                               @Value("${app.task-service.url:http://task-service:8083}") String taskServiceUrl) {
        this.restTemplate = restTemplate;
        this.taskServiceUrl = taskServiceUrl;
    }

    public void requireProjectAccess(UUID projectId, String bearerToken) {
        if (projectId == null) {
            throw new AppException("Project id is required", HttpStatus.BAD_REQUEST);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        try {
            restTemplate.exchange(
                    taskServiceUrl + "/api/v1/projects/" + projectId + "/members",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new AppException("Project access denied", HttpStatus.FORBIDDEN);
            }
            throw new AppException("Project access check failed", HttpStatus.BAD_GATEWAY);
        } catch (Exception exception) {
            throw new AppException("Project access check failed", HttpStatus.BAD_GATEWAY);
        }
    }
}
