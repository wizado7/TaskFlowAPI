package com.tasktracker.task.service;

import com.tasktracker.task.dto.TaskRequest;
import com.tasktracker.task.entity.BoardRole;
import com.tasktracker.task.entity.ProjectRole;
import com.tasktracker.task.entity.Task;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.repository.BoardMemberRepository;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.ProjectMemberRepository;
import com.tasktracker.task.repository.ProjectRepository;
import com.tasktracker.task.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository repository;
    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public TaskService(TaskRepository repository,
                       BoardRepository boardRepository,
                       BoardMemberRepository boardMemberRepository,
                       ProjectRepository projectRepository,
                       ProjectMemberRepository projectMemberRepository) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public Task create(TaskRequest request, String requesterEmail) {
        ensureCreateAccess(request, requesterEmail);
        Task task = new Task();
        apply(task, request);
        return repository.save(task);
    }

    public Task update(UUID id, TaskRequest request, String requesterEmail) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
        ensureUpdateAccess(task, request, requesterEmail);
        apply(task, request);
        return repository.save(task);
    }

    public Task get(UUID id, String requesterEmail) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
        ensureViewAccess(task, requesterEmail);
        return task;
    }

    public List<Task> listVisible(String userEmail) {
        List<Task> combined = new ArrayList<>(repository.findByAssigneeEmailsContaining(userEmail));

        List<UUID> boardIds = boardMemberRepository.findByUserEmail(userEmail).stream()
                .map(member -> member.getBoardId())
                .toList();
        if (!boardIds.isEmpty()) {
            combined.addAll(repository.findByBoardIdIn(boardIds));
        }

        List<UUID> ownerProjectIds = projectRepository.findByOwnerEmail(userEmail).stream()
                .map(project -> project.getId())
                .toList();
        if (!ownerProjectIds.isEmpty()) {
            combined.addAll(repository.findByProjectIdIn(ownerProjectIds));
        }

        return combined.stream()
                .collect(java.util.stream.Collectors.toMap(Task::getId, task -> task, (first, second) -> first))
                .values()
                .stream()
                .toList();
    }

    public void delete(UUID id, String requesterEmail) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
        UUID projectId = resolveProjectId(task);
        if (projectId == null || !isProjectOwner(projectId, requesterEmail)) {
            throw new AppException("Only project owner can delete tasks", HttpStatus.FORBIDDEN);
        }
        repository.deleteById(task.getId());
    }

    public List<Task> listBacklog(UUID boardId, String requesterEmail) {
        ensureBoardAccess(boardId, requesterEmail);
        return repository.findByBoardIdAndBacklogTrue(boardId);
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDeadline(request.deadline());
        task.getAssigneeEmails().clear();
        if (request.assigneeEmails() != null) {
            task.getAssigneeEmails().addAll(request.assigneeEmails());
        }
        task.setClientId(request.clientId());
        task.setProjectId(request.projectId());
        task.setBoardId(request.boardId());
        task.setColumnId(request.columnId());
        task.setSprintId(request.sprintId());
        task.setBacklog(request.backlog() != null && request.backlog());
    }

    private void ensureCreateAccess(TaskRequest request, String requesterEmail) {
        UUID projectId = request.projectId();
        UUID boardId = request.boardId();
        if (boardId != null) {
            ensureBoardAccess(boardId, requesterEmail);
            if (isBoardEditorOrOwner(boardId, requesterEmail)) {
                return;
            }
            UUID boardProjectId = boardRepository.findById(boardId)
                    .map(board -> board.getProjectId())
                    .orElse(null);
            projectId = projectId != null ? projectId : boardProjectId;
        }
        if (projectId == null) {
            throw new AppException("Project or board is required", HttpStatus.BAD_REQUEST);
        }
        ProjectRole role = resolveProjectRole(projectId, requesterEmail);
        if (role == ProjectRole.VIEWER) {
            throw new AppException("Viewers cannot create tasks", HttpStatus.FORBIDDEN);
        }
    }

    private void ensureUpdateAccess(Task task, TaskRequest request, String requesterEmail) {
        ensureViewAccess(task, requesterEmail);
        if (task.getBoardId() != null && isBoardEditorOrOwner(task.getBoardId(), requesterEmail)) {
            return;
        }
        UUID projectId = resolveProjectId(task);
        if (projectId == null) {
            throw new AppException("Project not found", HttpStatus.FORBIDDEN);
        }
        ProjectRole role = resolveProjectRole(projectId, requesterEmail);
        if (role == ProjectRole.LEAD) {
            return;
        }
        if (role == ProjectRole.EMPLOYEE) {
            if (!isStatusOnlyUpdate(task, request)) {
                throw new AppException("Employees can only update task status", HttpStatus.FORBIDDEN);
            }
            return;
        }
        throw new AppException("Task update not allowed", HttpStatus.FORBIDDEN);
    }

    private void ensureViewAccess(Task task, String requesterEmail) {
        if (task.getAssigneeEmails().stream().anyMatch(email -> email.equalsIgnoreCase(requesterEmail))) {
            return;
        }
        UUID projectId = resolveProjectId(task);
        if (projectId != null && isProjectOwner(projectId, requesterEmail)) {
            return;
        }
        if (task.getBoardId() != null && isBoardMember(task.getBoardId(), requesterEmail)) {
            return;
        }
        throw new AppException("Task access denied", HttpStatus.FORBIDDEN);
    }

    private boolean isStatusOnlyUpdate(Task task, TaskRequest request) {
        if (!Objects.equals(task.getStatus(), request.status())) {
            // allowed to change status
        }
        if (!Objects.equals(task.getTitle(), request.title())) {
            return false;
        }
        if (!Objects.equals(task.getDescription(), request.description())) {
            return false;
        }
        if (!Objects.equals(task.getPriority(), request.priority())) {
            return false;
        }
        if (!Objects.equals(task.getDeadline(), request.deadline())) {
            return false;
        }
        if (!Objects.equals(task.getClientId(), request.clientId())) {
            return false;
        }
        if (!Objects.equals(task.getProjectId(), request.projectId())) {
            return false;
        }
        if (!Objects.equals(task.getBoardId(), request.boardId())) {
            return false;
        }
        if (!Objects.equals(task.getColumnId(), request.columnId())) {
            return false;
        }
        if (!Objects.equals(task.getSprintId(), request.sprintId())) {
            return false;
        }
        boolean requestBacklog = request.backlog() != null && request.backlog();
        if (task.isBacklog() != requestBacklog) {
            return false;
        }
        Set<String> currentAssignees = new HashSet<>(task.getAssigneeEmails());
        Set<String> requestedAssignees = request.assigneeEmails() != null
                ? new HashSet<>(request.assigneeEmails())
                : new HashSet<>();
        return currentAssignees.equals(requestedAssignees);
    }

    private void ensureBoardAccess(UUID boardId, String requesterEmail) {
        boolean isMember = isBoardMember(boardId, requesterEmail);
        UUID projectId = boardRepository.findById(boardId)
                .map(board -> board.getProjectId())
                .orElse(null);
        if (projectId != null && isProjectOwner(projectId, requesterEmail)) {
            return;
        }
        if (!isMember) {
            throw new AppException("Board access denied", HttpStatus.FORBIDDEN);
        }
    }

    private boolean isBoardMember(UUID boardId, String requesterEmail) {
        return boardMemberRepository.findByBoardIdAndUserEmail(boardId, requesterEmail).isPresent();
    }

    private boolean isBoardEditorOrOwner(UUID boardId, String requesterEmail) {
        return boardMemberRepository.findByBoardIdAndUserEmail(boardId, requesterEmail)
                .map(member -> member.getRole() == BoardRole.OWNER || member.getRole() == BoardRole.EDITOR)
                .orElse(false);
    }

    private UUID resolveProjectId(Task task) {
        if (task.getProjectId() != null) {
            return task.getProjectId();
        }
        if (task.getBoardId() == null) {
            return null;
        }
        return boardRepository.findById(task.getBoardId())
                .map(board -> board.getProjectId())
                .orElse(null);
    }

    private ProjectRole resolveProjectRole(UUID projectId, String userEmail) {
        if (isProjectOwner(projectId, userEmail)) {
            return ProjectRole.LEAD;
        }
        return projectMemberRepository.findByProjectIdAndUserEmail(projectId, userEmail)
                .map(member -> member.getRole())
                .orElseThrow(() -> new AppException("Project access denied", HttpStatus.FORBIDDEN));
    }

    private boolean isProjectOwner(UUID projectId, String userEmail) {
        return projectRepository.findById(projectId)
                .map(project -> project.getOwnerEmail().equalsIgnoreCase(userEmail))
                .orElse(false);
    }
}
