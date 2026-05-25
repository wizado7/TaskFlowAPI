package com.tasktracker.task.service;

import com.tasktracker.task.entity.TaskAttachment;
import com.tasktracker.task.entity.Task;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.realtime.RealtimeAction;
import com.tasktracker.task.realtime.RealtimeResource;
import com.tasktracker.task.realtime.TaskRealtimeEvent;
import com.tasktracker.task.realtime.TaskRealtimePublisher;
import com.tasktracker.task.repository.TaskAttachmentRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskAttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L;
    private static final Map<String, Set<String>> ALLOWED_FILE_TYPES = Map.ofEntries(
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("txt", Set.of("text/plain")),
            Map.entry("csv", Set.of("text/csv", "application/csv", "application/vnd.ms-excel")),
            Map.entry("md", Set.of("text/markdown", "text/plain")),
            Map.entry("doc", Set.of("application/msword")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry("xls", Set.of("application/vnd.ms-excel")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry("ppt", Set.of("application/vnd.ms-powerpoint")),
            Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
    );

    private final TaskAttachmentRepository repository;
    private final TaskService taskService;
    private final TaskRealtimePublisher realtimePublisher;
    private final Path storageDir;

    public TaskAttachmentService(TaskAttachmentRepository repository,
                                 TaskService taskService,
                                 TaskRealtimePublisher realtimePublisher,
                                 @Value("${app.task-attachments.dir:uploads/task-attachments}") String storageDir) {
        this.repository = repository;
        this.taskService = taskService;
        this.realtimePublisher = realtimePublisher;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException exception) {
            throw new AppException("Attachment storage is not accessible", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public TaskAttachment upload(UUID taskId, String uploaderEmail, MultipartFile file) {
        Task task = taskService.get(taskId, uploaderEmail);

        if (file == null || file.isEmpty()) {
            throw new AppException("File is required", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException("File size exceeds 25 MB", HttpStatus.BAD_REQUEST);
        }

        String originalFileName = safeFileName(file.getOriginalFilename());
        String extension = extension(originalFileName);
        if (!isAllowedFileType(extension, file.getContentType())) {
            throw new AppException("This file type is not allowed", HttpStatus.BAD_REQUEST);
        }

        String storedFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path target = storageDir.resolve(storedFileName).normalize();

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new AppException("Failed to store attachment", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setUploaderEmail(uploaderEmail);
        attachment.setOriginalFileName(originalFileName);
        attachment.setStoredFileName(storedFileName);
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(file.getSize());
        TaskAttachment saved = repository.save(attachment);
        publish(task, saved.getId(), RealtimeAction.CREATED, uploaderEmail);
        return saved;
    }

    public List<TaskAttachment> list(UUID taskId, String requesterEmail) {
        taskService.get(taskId, requesterEmail);
        return repository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public List<TaskAttachment> listProjectAttachments(UUID projectId, String requesterEmail) {
        List<UUID> visibleTaskIds = taskService.listVisible(requesterEmail).stream()
                .filter(task -> projectId.equals(task.getProjectId()))
                .map(Task::getId)
                .toList();
        if (visibleTaskIds.isEmpty()) {
            return List.of();
        }
        return repository.findByTaskIdInOrderByCreatedAtDesc(visibleTaskIds);
    }

    public AttachmentDownload download(UUID taskId, UUID attachmentId, String requesterEmail) {
        taskService.get(taskId, requesterEmail);
        TaskAttachment attachment = repository.findById(attachmentId)
                .orElseThrow(() -> new AppException("Attachment not found", HttpStatus.NOT_FOUND));
        if (!attachment.getTaskId().equals(taskId)) {
            throw new AppException("Attachment not found", HttpStatus.NOT_FOUND);
        }
        Path filePath = storageDir.resolve(attachment.getStoredFileName()).normalize();
        if (!Files.exists(filePath)) {
            throw new AppException("Attachment file is missing", HttpStatus.NOT_FOUND);
        }
        Resource resource = new FileSystemResource(filePath);
        return new AttachmentDownload(
                resource,
                attachment.getOriginalFileName(),
                attachment.getContentType() == null || attachment.getContentType().isBlank()
                        ? "application/octet-stream"
                        : attachment.getContentType(),
                attachment.getSizeBytes()
        );
    }

    public void delete(UUID taskId, UUID attachmentId, String requesterEmail) {
        Task task = taskService.get(taskId, requesterEmail);
        TaskAttachment attachment = repository.findById(attachmentId)
                .orElseThrow(() -> new AppException("Attachment not found", HttpStatus.NOT_FOUND));
        if (!attachment.getTaskId().equals(taskId)) {
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
        repository.delete(attachment);
        publish(task, attachmentId, RealtimeAction.DELETED, requesterEmail);
    }

    private void publish(Task task, UUID attachmentId, RealtimeAction action, String actorEmail) {
        realtimePublisher.publish(TaskRealtimeEvent.of(
                RealtimeResource.ATTACHMENT,
                action,
                task.getProjectId(),
                task.getBoardId(),
                task.getId(),
                attachmentId,
                actorEmail
        ));
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

    private boolean isAllowedFileType(String extension, String contentType) {
        if (extension == null || extension.isBlank() || contentType == null || contentType.isBlank()) {
            return false;
        }
        Set<String> allowedContentTypes = ALLOWED_FILE_TYPES.get(extension.toLowerCase());
        String normalizedContentType = contentType.toLowerCase().split(";")[0].trim();
        return allowedContentTypes != null && allowedContentTypes.contains(normalizedContentType);
    }

    public record AttachmentDownload(Resource resource, String originalFileName, String contentType, long sizeBytes) {}
}
