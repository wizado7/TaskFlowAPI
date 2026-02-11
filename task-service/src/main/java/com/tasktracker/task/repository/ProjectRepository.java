package com.tasktracker.task.repository;

import com.tasktracker.task.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByOwnerEmail(String ownerEmail);
}
