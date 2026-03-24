package com.tasktracker.task.controller;

import com.tasktracker.task.dto.TaskCommentRequest;
import com.tasktracker.task.dto.TaskCommentResponse;
import com.tasktracker.task.dto.TaskAttachmentResponse;
import com.tasktracker.task.dto.TaskRequest;
import com.tasktracker.task.dto.TaskResponse;
import com.tasktracker.task.service.TaskAttachmentService;
import com.tasktracker.task.service.TaskCommentService;
import com.tasktracker.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService service;
    private final TaskCommentService commentService;
    private final TaskAttachmentService attachmentService;

    public TaskController(TaskService service,
                          TaskCommentService commentService,
                          TaskAttachmentService attachmentService) {
        this.service = service;
        this.commentService = commentService;
        this.attachmentService = attachmentService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(toResponse(service.create(request, jwt.getSubject())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable("id") UUID id,
                                               @AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(toResponse(service.update(id, request, jwt.getSubject())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> get(@PathVariable("id") UUID id,
                                            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(toResponse(service.get(id, jwt.getSubject())));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> myTasks(@AuthenticationPrincipal Jwt jwt) {
        var tasks = service.listVisible(jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/boards/{boardId}/backlog")
    public ResponseEntity<List<TaskResponse>> backlog(@PathVariable("boardId") UUID boardId,
                                                      @AuthenticationPrincipal Jwt jwt) {
        var tasks = service.listBacklog(boardId, jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(tasks);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id,
                                       @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskCommentResponse> addComment(@PathVariable("id") UUID id,
                                                          @AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody TaskCommentRequest request) {
        var comment = commentService.addComment(id, jwt.getSubject(), request);
        return ResponseEntity.ok(toResponse(comment));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<TaskCommentResponse>> listComments(@PathVariable("id") UUID id,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        var comments = commentService.listComments(id, jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/{id}/comments/{commentId}")
    public ResponseEntity<TaskCommentResponse> updateComment(@PathVariable("id") UUID id,
                                                             @PathVariable("commentId") UUID commentId,
                                                             @AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody TaskCommentRequest request) {
        var comment = commentService.updateComment(id, commentId, jwt.getSubject(), request);
        return ResponseEntity.ok(toResponse(comment));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable("id") UUID id,
                                              @PathVariable("commentId") UUID commentId,
                                              @AuthenticationPrincipal Jwt jwt) {
        commentService.deleteComment(id, commentId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskAttachmentResponse> uploadAttachment(@PathVariable("id") UUID id,
                                                                   @AuthenticationPrincipal Jwt jwt,
                                                                   @RequestParam("file") MultipartFile file) {
        var attachment = attachmentService.upload(id, jwt.getSubject(), file);
        return ResponseEntity.ok(toResponse(attachment));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<TaskAttachmentResponse>> listAttachments(@PathVariable("id") UUID id,
                                                                        @AuthenticationPrincipal Jwt jwt) {
        var attachments = attachmentService.list(id, jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") UUID id,
                                                       @PathVariable("attachmentId") UUID attachmentId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var download = attachmentService.download(id, attachmentId, jwt.getSubject());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.contentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.originalFileName()).build().toString())
                .body(download.resource());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable("id") UUID id,
                                                 @PathVariable("attachmentId") UUID attachmentId,
                                                 @AuthenticationPrincipal Jwt jwt) {
        attachmentService.delete(id, attachmentId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    private TaskResponse toResponse(com.tasktracker.task.entity.Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getStartDate(),
                task.getEndDate(),
                task.getAssigneeEmails().stream().toList(),
                task.getClientId(),
                task.getProjectId(),
                task.getBoardId(),
                task.getColumnId(),
                task.getSprintId(),
                task.isBacklog()
        );
    }

    private TaskCommentResponse toResponse(com.tasktracker.task.entity.TaskComment comment) {
        return new TaskCommentResponse(
                comment.getId(),
                comment.getTaskId(),
                comment.getAuthorEmail(),
                comment.getMessage(),
                comment.getCreatedAt()
        );
    }

    private TaskAttachmentResponse toResponse(com.tasktracker.task.entity.TaskAttachment attachment) {
        return new TaskAttachmentResponse(
                attachment.getId(),
                attachment.getTaskId(),
                attachment.getUploaderEmail(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getCreatedAt()
        );
    }
}
