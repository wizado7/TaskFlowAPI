package com.tasktracker.auth.config;

import com.tasktracker.auth.service.OAuthLoginCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OAuthLoginCodeCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OAuthLoginCodeCleanupJob.class);

    private final OAuthLoginCodeService service;

    public OAuthLoginCodeCleanupJob(OAuthLoginCodeService service) {
        this.service = service;
    }

    @Scheduled(
            fixedDelayString = "${app.oauth.login-code-cleanup-delay-ms:300000}",
            initialDelayString = "${app.oauth.login-code-cleanup-initial-delay-ms:60000}"
    )
    public void cleanupExpiredCodes() {
        long deleted = service.cleanupExpiredCodes(Instant.now());
        if (deleted > 0) {
            log.info("OAuth login code cleanup removed {} expired codes", deleted);
        }
    }
}
