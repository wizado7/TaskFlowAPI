package com.tasktracker.client.repository;

import com.tasktracker.client.entity.ClientAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClientAttachmentRepository extends JpaRepository<ClientAttachment, UUID> {
    List<ClientAttachment> findByClientIdOrderByCreatedAtAsc(UUID clientId);
    void deleteByClientId(UUID clientId);
}
