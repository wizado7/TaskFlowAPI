package com.tasktracker.task.controller;

import com.tasktracker.task.dto.TaskCommentRequest;
import com.tasktracker.task.dto.TaskCommentResponse;
import com.tasktracker.task.dto.TaskRequest;
import com.tasktracker.task.dto.TaskResponse;
import com.tasktracker.task.service.TaskCommentService;
import com.tasktracker.task.service.TaskService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService service;
    private final TaskCommentService commentService;

    public TaskController(TaskService service, TaskCommentService commentService) {
        this.service = service;
        this.commentService = commentService;
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

    private TaskResponse toResponse(com.tasktracker.task.entity.Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
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
}
