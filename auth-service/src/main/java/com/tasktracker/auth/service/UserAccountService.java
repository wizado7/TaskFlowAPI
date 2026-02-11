package com.tasktracker.auth.service;

import com.tasktracker.auth.dto.AdminCreateRequest;
import com.tasktracker.auth.dto.LoginRequest;
import com.tasktracker.auth.dto.RegisterRequest;
import com.tasktracker.auth.entity.AuthProvider;
import com.tasktracker.auth.entity.Role;
import com.tasktracker.auth.entity.UserAccount;
import com.tasktracker.auth.exception.AppException;
import com.tasktracker.auth.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserProfileProvisioner profileProvisioner;

    public UserAccountService(UserAccountRepository repository,
                              PasswordEncoder passwordEncoder,
                              AuthenticationManager authenticationManager,
                              UserProfileProvisioner profileProvisioner) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.profileProvisioner = profileProvisioner;
    }

    public UserAccount register(RegisterRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }
        UserAccount user = new UserAccount();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setProvider(AuthProvider.LOCAL);
        user.getRoles().add(Role.USER);
        UserAccount saved = repository.save(user);
        profileProvisioner.provision(saved.getEmail());
        return saved;
    }

    public void authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
    }

    public UserAccount getByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    public UserAccount createAdmin(AdminCreateRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }
        UserAccount admin = new UserAccount();
        admin.setEmail(request.email());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setProvider(AuthProvider.LOCAL);
        admin.setBlocked(false);
        admin.getRoles().add(Role.ADMIN);
        admin.getRoles().add(Role.USER);
        UserAccount saved = repository.save(admin);
        profileProvisioner.provision(saved.getEmail());
        return saved;
    }

    public UserAccount updateRoles(UUID userId, Set<Role> roles) {
        UserAccount user = repository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        user.setRoles(roles);
        return repository.save(user);
    }

    public UserAccount blockUser(UUID userId, boolean blocked) {
        UserAccount user = repository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        user.setBlocked(blocked);
        return repository.save(user);
    }
}
