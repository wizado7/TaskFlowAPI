package com.tasktracker.task.repository;

import com.tasktracker.task.entity.ProjectInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, UUID> {
    Optional<ProjectInvite> findByToken(String token);
    Optional<ProjectInvite> findByProjectIdAndUserEmail(UUID projectId, String userEmail);
    void deleteByProjectId(UUID projectId);
}
