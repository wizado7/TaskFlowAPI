package com.tasktracker.task.repository;

import com.tasktracker.task.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, UUID> {
    List<TaskAttachment> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
