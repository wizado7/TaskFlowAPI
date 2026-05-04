package com.tasktracker.task.service;

import com.tasktracker.task.dto.TaskRequest;
import com.tasktracker.task.entity.BoardRole;
import com.tasktracker.task.entity.BoardType;
import com.tasktracker.task.entity.ProjectRole;
import com.tasktracker.task.entity.SprintStatus;
import com.tasktracker.task.entity.Task;
import com.tasktracker.task.entity.TaskStatus;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.realtime.RealtimeAction;
import com.tasktracker.task.realtime.RealtimeResource;
import com.tasktracker.task.realtime.TaskRealtimeEvent;
import com.tasktracker.task.realtime.TaskRealtimePublisher;
import com.tasktracker.task.repository.BoardMemberRepository;
import com.tasktracker.task.repository.BoardColumnRepository;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.ProjectMemberRepository;
import com.tasktracker.task.repository.ProjectRepository;
import com.tasktracker.task.repository.SprintRepository;
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
    private final BoardColumnRepository boardColumnRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SprintRepository sprintRepository;
    private final TaskRealtimePublisher realtimePublisher;

    public TaskService(TaskRepository repository,
                       BoardRepository boardRepository,
                       BoardColumnRepository boardColumnRepository,
                       BoardMemberRepository boardMemberRepository,
                       ProjectRepository projectRepository,
                       ProjectMemberRepository projectMemberRepository,
                       SprintRepository sprintRepository,
                       TaskRealtimePublisher realtimePublisher) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.sprintRepository = sprintRepository;
        this.realtimePublisher = realtimePublisher;
    }

    public Task create(TaskRequest request, String requesterEmail) {
        ensureCreateAccess(request, requesterEmail);
        if (request.storyPoints() != null && request.boardId() != null) {
            ensureBoardOwner(request.boardId(), requesterEmail);
        }
        Task task = new Task();
        apply(task, request);
        Task saved = repository.save(task);
        publish(saved, RealtimeAction.CREATED, requesterEmail);
        return saved;
    }

    public Task update(UUID id, TaskRequest request, String requesterEmail) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
        ensureUpdateAccess(task, request, requesterEmail);
        if (request.storyPoints() != null && !Objects.equals(task.getStoryPoints(), request.storyPoints())) {
            if (task.getBoardId() != null) {
                ensureBoardOwner(task.getBoardId(), requesterEmail);
            } else {
                UUID projectId = resolveProjectId(task);
                if (projectId == null || !isProjectOwner(projectId, requesterEmail)) {
                    throw new AppException("Only board owner can estimate tasks", HttpStatus.FORBIDDEN);
                }
            }
        }
        apply(task, request);
        Task saved = repository.save(task);
        publish(saved, RealtimeAction.UPDATED, requesterEmail);
        return saved;
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
        UUID boardId = task.getBoardId();
        if (!canDeleteTask(projectId, boardId, requesterEmail)) {
            throw new AppException("Only board creator, board editor, board owner, or project owner can delete tasks", HttpStatus.FORBIDDEN);
        }
        UUID taskId = task.getId();
        repository.deleteById(task.getId());
        realtimePublisher.publish(TaskRealtimeEvent.of(
                RealtimeResource.TASK,
                RealtimeAction.DELETED,
                projectId,
                boardId,
                taskId,
                taskId,
                requesterEmail
        ));
    }

    private boolean canDeleteTask(UUID projectId, UUID boardId, String requesterEmail) {
        if (projectId != null && isProjectOwner(projectId, requesterEmail)) {
            return true;
        }
        if (boardId == null) {
            return false;
        }
        var board = boardRepository.findById(boardId)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        if (board.getCreatedBy().equalsIgnoreCase(requesterEmail)) {
            return true;
        }
        return boardMemberRepository.findByBoardIdAndUserEmail(boardId, requesterEmail)
                .map(member -> member.getRole() == BoardRole.OWNER || member.getRole() == BoardRole.EDITOR)
                .orElse(false);
    }

    public List<Task> listBacklog(UUID boardId, String requesterEmail) {
        ensureBoardAccess(boardId, requesterEmail);
        return repository.findByBoardId(boardId).stream()
                .filter(task -> task.getSprintId() == null || task.isBacklog())
                .toList();
    }

    public List<Task> listSprintTasks(UUID sprintId, String requesterEmail) {
        var sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new AppException("Sprint not found", HttpStatus.NOT_FOUND));
        ensureBoardAccess(sprint.getBoardId(), requesterEmail);
        return repository.findBySprintId(sprintId);
    }

    public Task estimate(UUID id, Integer storyPoints, String requesterEmail) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
        UUID boardId = task.getBoardId();
        if (boardId == null) {
            UUID projectId = resolveProjectId(task);
            if (projectId == null || !isProjectOwner(projectId, requesterEmail)) {
                throw new AppException("Only board owner or project lead can estimate tasks", HttpStatus.FORBIDDEN);
            }
        } else {
            ensureBoardOwner(boardId, requesterEmail);
        }
        task.setStoryPoints(storyPoints);
        Task saved = repository.save(task);
        publish(saved, RealtimeAction.UPDATED, requesterEmail);
        return saved;
    }

    public Task addToSprint(UUID sprintId, UUID taskId, String requesterEmail) {
        var sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new AppException("Sprint not found", HttpStatus.NOT_FOUND));
        ensureBoardOwner(sprint.getBoardId(), requesterEmail);
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
        var sprintBoard = boardRepository.findById(sprint.getBoardId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        ensureBacklogPlanningAccess(sprintBoard, requesterEmail);
        if (task.getBoardId() != null && !task.getBoardId().equals(sprint.getBoardId())) {
            var sourceBoard = boardRepository.findById(task.getBoardId())
                    .orElseThrow(() -> new AppException("Task board not found", HttpStatus.NOT_FOUND));
            if (sourceBoard.getType() != BoardType.BACKLOG || !sourceBoard.getProjectId().equals(sprintBoard.getProjectId())) {
                throw new AppException("Task belongs to another board", HttpStatus.CONFLICT);
            }
        }
        task.setProjectId(sprintBoard.getProjectId());
        task.setBoardId(sprint.getBoardId());
        task.setSprintId(sprint.getId());
        task.setColumnId(firstColumnId(sprint.getBoardId()));
        task.setBacklog(false);
        Task saved = repository.save(task);
        publish(saved, RealtimeAction.UPDATED, requesterEmail);
        return saved;
    }

    public Task removeFromSprint(UUID sprintId, UUID taskId, String requesterEmail) {
        var sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new AppException("Sprint not found", HttpStatus.NOT_FOUND));
        ensureBoardOwner(sprint.getBoardId(), requesterEmail);
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new AppException("Task not found", HttpStatus.NOT_FOUND));
        if (!sprintId.equals(task.getSprintId())) {
            throw new AppException("Task is not in this sprint", HttpStatus.CONFLICT);
        }
        task.setSprintId(null);
        task.setColumnId(null);
        task.setBacklog(true);
        Task saved = repository.save(task);
        publish(saved, RealtimeAction.UPDATED, requesterEmail);
        return saved;
    }

    public void moveUnfinishedSprintTasksToBacklog(UUID sprintId, UUID targetBoardId, String requesterEmail) {
        var sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new AppException("Sprint not found", HttpStatus.NOT_FOUND));
        UUID boardId = resolveBacklogBoardId(sprint.getBoardId());
        UUID columnId = firstColumnId(boardId);
        List<Task> tasks = repository.findBySprintId(sprintId);
        for (Task task : tasks) {
            if (task.getStatus() == TaskStatus.DONE) {
                continue;
            }
            task.setBoardId(boardId);
            task.setSprintId(null);
            task.setColumnId(columnId);
            task.setBacklog(true);
            Task saved = repository.save(task);
            publish(saved, RealtimeAction.UPDATED, requesterEmail);
        }
    }

    private void ensureBacklogPlanningAccess(com.tasktracker.task.entity.Board board, String requesterEmail) {
        if (board.getCreatedBy().equalsIgnoreCase(requesterEmail) || isProjectOwner(board.getProjectId(), requesterEmail)) {
            return;
        }
        throw new AppException("Only board creator or project owner can move backlog tasks to sprint", HttpStatus.FORBIDDEN);
    }

    private UUID resolveBacklogBoardId(UUID sprintBoardId) {
        var sprintBoard = boardRepository.findById(sprintBoardId)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        return boardRepository.findByProjectIdAndType(sprintBoard.getProjectId(), BoardType.BACKLOG).stream()
                .findFirst()
                .map(com.tasktracker.task.entity.Board::getId)
                .orElseThrow(() -> new AppException("Project backlog board not found", HttpStatus.CONFLICT));
    }

    private UUID firstColumnId(UUID boardId) {
        return boardColumnRepository.findByBoardIdOrderByPositionAsc(boardId).stream()
                .findFirst()
                .map(com.tasktracker.task.entity.BoardColumn::getId)
                .orElse(null);
    }

    public int plannedPoints(UUID sprintId) {
        return repository.findBySprintId(sprintId).stream()
                .map(Task::getStoryPoints)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int completedPoints(UUID sprintId) {
        return repository.findBySprintId(sprintId).stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .map(Task::getStoryPoints)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setStartDate(request.startDate());
        task.setEndDate(request.endDate());
        task.getAssigneeEmails().clear();
        if (request.assigneeEmails() != null) {
            task.getAssigneeEmails().addAll(request.assigneeEmails());
        }
        task.setClientId(request.clientId());
        task.setProjectId(request.projectId());
        task.setBoardId(request.boardId());
        task.setColumnId(request.columnId());
        task.setSprintId(request.sprintId());
        if (request.storyPoints() != null || task.getStoryPoints() == null) {
            task.setStoryPoints(request.storyPoints());
        }
        task.setBacklog(request.backlog() != null ? request.backlog() : request.sprintId() == null);
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
        if (!Objects.equals(task.getStartDate(), request.startDate())) {
            return false;
        }
        if (!Objects.equals(task.getEndDate(), request.endDate())) {
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
        if (request.storyPoints() != null && !Objects.equals(task.getStoryPoints(), request.storyPoints())) {
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

    private void ensureBoardOwner(UUID boardId, String requesterEmail) {
        var board = boardRepository.findById(boardId)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        if (isProjectOwner(board.getProjectId(), requesterEmail)) {
            return;
        }
        if (boardMemberRepository.findByBoardIdAndUserEmail(boardId, requesterEmail)
                .map(member -> member.getRole() == BoardRole.OWNER)
                .orElse(false)) {
            return;
        }
        throw new AppException("Only board owner can estimate and plan sprint tasks", HttpStatus.FORBIDDEN);
    }

    private void publish(Task task, RealtimeAction action, String requesterEmail) {
        realtimePublisher.publish(TaskRealtimeEvent.of(
                RealtimeResource.TASK,
                action,
                resolveProjectId(task),
                task.getBoardId(),
                task.getId(),
                task.getId(),
                requesterEmail
        ));
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
