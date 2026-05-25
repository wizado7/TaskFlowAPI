package com.tasktracker.client.service;

import com.tasktracker.client.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProjectAccessClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private ProjectAccessClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new ProjectAccessClient(restTemplate, "http://task-service:8083");
    }

    @Test
    void requireProjectAccessPassesBearerTokenToTaskService() {
        UUID projectId = UUID.randomUUID();
        server.expect(requestTo("http://task-service:8083/api/v1/projects/" + projectId + "/members"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess());

        client.requireProjectAccess(projectId, "access-token");

        server.verify();
    }

    @Test
    void requireProjectAccessMapsClientErrorsToForbidden() {
        UUID projectId = UUID.randomUUID();
        server.expect(requestTo("http://task-service:8083/api/v1/projects/" + projectId + "/members"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.requireProjectAccess(projectId, "access-token"))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requireProjectAccessMapsServerErrorsToBadGateway() {
        UUID projectId = UUID.randomUUID();
        server.expect(requestTo("http://task-service:8083/api/v1/projects/" + projectId + "/members"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.requireProjectAccess(projectId, "access-token"))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
