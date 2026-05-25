package com.tasktracker.task.service;

import com.tasktracker.task.entity.Task;
import com.tasktracker.task.entity.TaskAttachment;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.realtime.TaskRealtimePublisher;
import com.tasktracker.task.repository.TaskAttachmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {

    @TempDir
    private Path storageDir;

    @Mock
    private TaskAttachmentRepository repository;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskRealtimePublisher realtimePublisher;

    @Test
    void uploadStoresAllowedFileWithSanitizedOriginalName() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = task(taskId);
        when(taskService.get(taskId, "user@example.com")).thenReturn(task);
        when(repository.save(any(TaskAttachment.class))).thenAnswer(invocation -> {
            TaskAttachment attachment = invocation.getArgument(0);
            attachment.setId(UUID.randomUUID());
            return attachment;
        });
        TaskAttachmentService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../report.pdf",
                "application/pdf",
                "content".getBytes()
        );

        TaskAttachment saved = service.upload(taskId, "user@example.com", file);

        assertThat(saved.getOriginalFileName()).isEqualTo(".._.._report.pdf");
        assertThat(saved.getStoredFileName()).endsWith(".pdf");
        assertThat(Files.exists(storageDir.resolve(saved.getStoredFileName()))).isTrue();
        verify(repository).save(any(TaskAttachment.class));
    }

    @Test
    void uploadRejectsDisallowedFileType() {
        UUID taskId = UUID.randomUUID();
        when(taskService.get(taskId, "user@example.com")).thenReturn(task(taskId));
        TaskAttachmentService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.exe",
                "application/x-msdownload",
                "content".getBytes()
        );

        assertThatThrownBy(() -> service.upload(taskId, "user@example.com", file))
                .isInstanceOf(AppException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repository, never()).save(any(TaskAttachment.class));
    }

    private TaskAttachmentService service() {
        return new TaskAttachmentService(repository, taskService, realtimePublisher, storageDir.toString());
    }

    private Task task(UUID taskId) {
        Task task = new Task();
        task.setId(taskId);
        task.setProjectId(UUID.randomUUID());
        task.setBoardId(UUID.randomUUID());
        task.setTitle("Task");
        return task;
    }
}
