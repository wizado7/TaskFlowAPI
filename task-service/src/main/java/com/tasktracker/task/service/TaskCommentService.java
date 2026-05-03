package com.tasktracker.task.service;

import com.tasktracker.task.dto.TaskCommentRequest;
import com.tasktracker.task.entity.Task;
import com.tasktracker.task.entity.TaskComment;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.realtime.RealtimeAction;
import com.tasktracker.task.realtime.RealtimeResource;
import com.tasktracker.task.realtime.TaskRealtimeEvent;
import com.tasktracker.task.realtime.TaskRealtimePublisher;
import com.tasktracker.task.repository.TaskCommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TaskCommentService {

    private final TaskCommentRepository repository;
    private final TaskService taskService;
    private final TaskRealtimePublisher realtimePublisher;

    public TaskCommentService(TaskCommentRepository repository,
                              TaskService taskService,
                              TaskRealtimePublisher realtimePublisher) {
        this.repository = repository;
        this.taskService = taskService;
        this.realtimePublisher = realtimePublisher;
    }

    public TaskComment addComment(UUID taskId, String authorEmail, TaskCommentRequest request) {
        Task task = taskService.get(taskId, authorEmail);
        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setAuthorEmail(authorEmail);
        comment.setMessage(request.message());
        TaskComment saved = repository.save(comment);
        publish(task, saved.getId(), RealtimeAction.CREATED, authorEmail);
        return saved;
    }

    public List<TaskComment> listComments(UUID taskId, String requesterEmail) {
        taskService.get(taskId, requesterEmail);
        return repository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public TaskComment updateComment(UUID taskId, UUID commentId, String requesterEmail, TaskCommentRequest request) {
        Task task = taskService.get(taskId, requesterEmail);
        TaskComment comment = repository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment not found", HttpStatus.NOT_FOUND));
        if (!comment.getTaskId().equals(taskId)) {
            throw new AppException("Comment not found", HttpStatus.NOT_FOUND);
        }
        if (!comment.getAuthorEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AppException("Only comment author can edit it", HttpStatus.FORBIDDEN);
        }
        comment.setMessage(request.message());
        TaskComment saved = repository.save(comment);
        publish(task, saved.getId(), RealtimeAction.UPDATED, requesterEmail);
        return saved;
    }

    public void deleteComment(UUID taskId, UUID commentId, String requesterEmail) {
        Task task = taskService.get(taskId, requesterEmail);
        TaskComment comment = repository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment not found", HttpStatus.NOT_FOUND));
        if (!comment.getTaskId().equals(taskId)) {
            throw new AppException("Comment not found", HttpStatus.NOT_FOUND);
        }
        if (!comment.getAuthorEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AppException("Only comment author can delete it", HttpStatus.FORBIDDEN);
        }
        repository.delete(comment);
        publish(task, commentId, RealtimeAction.DELETED, requesterEmail);
    }

    private void publish(Task task, UUID commentId, RealtimeAction action, String actorEmail) {
        realtimePublisher.publish(TaskRealtimeEvent.of(
                RealtimeResource.COMMENT,
                action,
                task.getProjectId(),
                task.getBoardId(),
                task.getId(),
                commentId,
                actorEmail
        ));
    }
}
