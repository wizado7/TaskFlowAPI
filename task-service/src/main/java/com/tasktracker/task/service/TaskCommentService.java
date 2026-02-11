package com.tasktracker.task.service;

import com.tasktracker.task.dto.TaskCommentRequest;
import com.tasktracker.task.entity.TaskComment;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.repository.TaskCommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TaskCommentService {

    private final TaskCommentRepository repository;
    private final TaskService taskService;

    public TaskCommentService(TaskCommentRepository repository,
                              TaskService taskService) {
        this.repository = repository;
        this.taskService = taskService;
    }

    public TaskComment addComment(UUID taskId, String authorEmail, TaskCommentRequest request) {
        taskService.get(taskId, authorEmail);
        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setAuthorEmail(authorEmail);
        comment.setMessage(request.message());
        return repository.save(comment);
    }

    public List<TaskComment> listComments(UUID taskId, String requesterEmail) {
        taskService.get(taskId, requesterEmail);
        return repository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }
}
