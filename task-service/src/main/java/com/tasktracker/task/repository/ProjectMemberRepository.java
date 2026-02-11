package com.tasktracker.task.repository;

import com.tasktracker.task.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    Optional<ProjectMember> findByProjectIdAndUserEmail(UUID projectId, String userEmail);
    List<ProjectMember> findByProjectId(UUID projectId);
    List<ProjectMember> findByUserEmail(String userEmail);
    void deleteByProjectId(UUID projectId);
}
