package com.tasktracker.client.service;

import com.tasktracker.client.dto.ClientCreateRequest;
import com.tasktracker.client.dto.ClientUpdateRequest;
import com.tasktracker.client.entity.Client;
import com.tasktracker.client.entity.ClientAttachment;
import com.tasktracker.client.entity.ClientComment;
import com.tasktracker.client.entity.CrmStage;
import com.tasktracker.client.exception.AppException;
import com.tasktracker.client.repository.ClientAttachmentRepository;
import com.tasktracker.client.repository.ClientCommentRepository;
import com.tasktracker.client.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ClientService {

    private static final long MAX_FILE_SIZE_BYTES = 100L * 1024L * 1024L;
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of("exe", "bat", "cmd");

    private final ClientRepository repository;
    private final ClientCommentRepository commentRepository;
    private final ClientAttachmentRepository attachmentRepository;
    private final Path storageDir;

    public ClientService(ClientRepository repository,
                         ClientCommentRepository commentRepository,
                         ClientAttachmentRepository attachmentRepository,
                         @Value("${app.client-attachments.dir:uploads/client-attachments}") String storageDir) {
        this.repository = repository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException exception) {
            throw new AppException("Client attachment storage is not accessible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Client create(ClientCreateRequest request, String creatorEmail) {
        Client client = new Client();
        client.setProjectId(request.projectId());
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setCompany(request.company());
        client.setNotes(request.notes());
        client.setStage(resolveStage(request.stage(), CrmStage.LEAD));
        client.setCreatedBy(creatorEmail);
        return repository.save(client);
    }

    public Client update(UUID id, ClientUpdateRequest request) {
        Client client = repository.findById(id)
                .orElseThrow(() -> new AppException("Client not found", HttpStatus.NOT_FOUND));
        if (request.projectId() != null) client.setProjectId(request.projectId());
        if (request.name() != null) client.setName(request.name());
        if (request.email() != null) client.setEmail(request.email());
        if (request.phone() != null) client.setPhone(request.phone());
        if (request.company() != null) client.setCompany(request.company());
        if (request.notes() != null) client.setNotes(request.notes());
        if (request.stage() != null) client.setStage(resolveStage(request.stage(), client.getStage()));
        return repository.save(client);
    }

    public Client get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException("Client not found", HttpStatus.NOT_FOUND));
    }

    public List<Client> list(UUID projectId) {
        return projectId == null ? repository.findAll() : repository.findByProjectId(projectId);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new AppException("Client not found", HttpStatus.NOT_FOUND);
        }
        attachmentRepository.deleteByClientId(id);
        commentRepository.deleteByClientId(id);
        repository.deleteById(id);
    }

    public ClientComment addComment(UUID clientId, String authorEmail, String message) {
        get(clientId);
        if (message == null || message.isBlank()) {
            throw new AppException("Comment is required", HttpStatus.BAD_REQUEST);
        }
        ClientComment comment = new ClientComment();
        comment.setClientId(clientId);
        comment.setAuthorEmail(authorEmail);
        comment.setMessage(message.trim());
        return commentRepository.save(comment);
    }

    public List<ClientComment> listComments(UUID clientId) {
        get(clientId);
        return commentRepository.findByClientIdOrderByCreatedAtAsc(clientId);
    }

    public ClientAttachment uploadAttachment(UUID clientId, String uploaderEmail, MultipartFile file) {
        get(clientId);
        if (file == null || file.isEmpty()) {
            throw new AppException("File is required", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException("File size exceeds 100 MB", HttpStatus.BAD_REQUEST);
        }

        String originalFileName = safeFileName(file.getOriginalFilename());
        String extension = extension(originalFileName);
        if (FORBIDDEN_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new AppException("This file type is not allowed", HttpStatus.BAD_REQUEST);
        }

        String storedFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path target = storageDir.resolve(storedFileName).normalize();

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new AppException("Failed to store attachment", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ClientAttachment attachment = new ClientAttachment();
        attachment.setClientId(clientId);
        attachment.setUploaderEmail(uploaderEmail);
        attachment.setOriginalFileName(originalFileName);
        attachment.setStoredFileName(storedFileName);
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(file.getSize());
        return attachmentRepository.save(attachment);
    }

    public List<ClientAttachment> listAttachments(UUID clientId) {
        get(clientId);
        return attachmentRepository.findByClientIdOrderByCreatedAtAsc(clientId);
    }

    public AttachmentDownload downloadAttachment(UUID clientId, UUID attachmentId) {
        get(clientId);
        ClientAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException("Attachment not found", HttpStatus.NOT_FOUND));
        if (!attachment.getClientId().equals(clientId)) {
            throw new AppException("Attachment not found", HttpStatus.NOT_FOUND);
        }
        Path filePath = storageDir.resolve(attachment.getStoredFileName()).normalize();
        if (!Files.exists(filePath)) {
            throw new AppException("Attachment file is missing", HttpStatus.NOT_FOUND);
        }
        return new AttachmentDownload(
                new FileSystemResource(filePath),
                attachment.getOriginalFileName(),
                attachment.getContentType() == null || attachment.getContentType().isBlank()
                        ? "application/octet-stream"
                        : attachment.getContentType(),
                attachment.getSizeBytes()
        );
    }

    public void deleteAttachment(UUID clientId, UUID attachmentId, String requesterEmail) {
        get(clientId);
        ClientAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException("Attachment not found", HttpStatus.NOT_FOUND));
        if (!attachment.getClientId().equals(clientId)) {
            throw new AppException("Attachment not found", HttpStatus.NOT_FOUND);
        }
        if (!attachment.getUploaderEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AppException("Only uploader can delete attachment", HttpStatus.FORBIDDEN);
        }
        Path filePath = storageDir.resolve(attachment.getStoredFileName()).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new AppException("Failed to delete attachment file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        attachmentRepository.delete(attachment);
    }

    private CrmStage resolveStage(String value, CrmStage defaultStage) {
        if (value == null || value.isBlank()) {
            return defaultStage;
        }
        try {
            return CrmStage.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new AppException("Unknown CRM stage", HttpStatus.BAD_REQUEST);
        }
    }

    private String safeFileName(String originalFileName) {
        String candidate = originalFileName == null || originalFileName.isBlank() ? "file" : originalFileName;
        return candidate.replace("\\", "_").replace("/", "_").trim();
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1);
    }

    public record AttachmentDownload(Resource resource, String originalFileName, String contentType, long sizeBytes) {}
}
