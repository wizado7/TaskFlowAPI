package com.tasktracker.client.repository;

import com.tasktracker.client.entity.ClientComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClientCommentRepository extends JpaRepository<ClientComment, UUID> {
    List<ClientComment> findByClientIdOrderByCreatedAtAsc(UUID clientId);
    void deleteByClientId(UUID clientId);
}
