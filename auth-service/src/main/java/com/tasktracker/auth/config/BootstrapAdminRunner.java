package com.tasktracker.auth.config;

import com.tasktracker.auth.entity.AuthProvider;
import com.tasktracker.auth.entity.Role;
import com.tasktracker.auth.entity.UserAccount;
import com.tasktracker.auth.repository.UserAccountRepository;
import com.tasktracker.auth.service.UserProfileProvisioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileProvisioner profileProvisioner;

    @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}")
    private String adminPassword;

    public BootstrapAdminRunner(UserAccountRepository repository,
                                PasswordEncoder passwordEncoder,
                                UserProfileProvisioner profileProvisioner) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.profileProvisioner = profileProvisioner;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        Optional<UserAccount> existing = repository.findByEmail(adminEmail);
        if (existing.isPresent()) {
            UserAccount user = existing.get();
            user.getRoles().add(Role.ADMIN);
            user.getRoles().add(Role.USER);
            repository.save(user);
            profileProvisioner.provision(user.getEmail());
            log.info("Bootstrap admin: granted ADMIN role to existing user {}", adminEmail);
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setProvider(AuthProvider.LOCAL);
        admin.setBlocked(false);
        admin.setRoles(EnumSet.of(Role.ADMIN, Role.USER));
        repository.save(admin);
        profileProvisioner.provision(admin.getEmail());
        log.info("Bootstrap admin: created admin user {}", adminEmail);
    }
}
