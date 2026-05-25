package com.tasktracker.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            @Value("${app.security.public-api-docs-enabled:false}") boolean publicApiDocsEnabled
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> {
                    exchange.pathMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/ws/task-events",
                        "/api/v1/users/*/avatar"
                    ).permitAll();
                    if (publicApiDocsEnabled) {
                        exchange.pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/auth-service/v3/api-docs/**",
                                "/user-service/v3/api-docs/**",
                                "/task-service/v3/api-docs/**",
                                "/client-service/v3/api-docs/**"
                        ).permitAll();
                    }
                    exchange.pathMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                            .pathMatchers("/oauth2/**", "/login/**").permitAll()
                            .anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
