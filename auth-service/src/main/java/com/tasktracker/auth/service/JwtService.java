package com.tasktracker.auth.service;

import com.tasktracker.auth.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {

    private final String secret;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String subject, Set<Role> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
            .setIssuer(issuer)
                .setSubject(subject)
                .claim("roles", roles.stream().map(Enum::name).toList())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60L;
    }
}
