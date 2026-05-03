package com.tasktracker.task.service;

import com.tasktracker.task.dto.BoardRequest;
import com.tasktracker.task.dto.BoardUpdateRequest;
import com.tasktracker.task.entity.Board;
import com.tasktracker.task.entity.BoardColumn;
import com.tasktracker.task.entity.BoardMethodology;
import com.tasktracker.task.entity.BoardMember;
import com.tasktracker.task.entity.BoardRole;
import com.tasktracker.task.entity.BoardType;
import com.tasktracker.task.entity.Sprint;
import com.tasktracker.task.entity.SprintStatus;
import com.tasktracker.task.entity.Task;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.realtime.RealtimeAction;
import com.tasktracker.task.realtime.RealtimeResource;
import com.tasktracker.task.realtime.TaskRealtimeEvent;
import com.tasktracker.task.realtime.TaskRealtimePublisher;
import com.tasktracker.task.repository.BoardColumnRepository;
import com.tasktracker.task.repository.BoardMemberRepository;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.ProjectRepository;
import com.tasktracker.task.repository.ProjectMemberRepository;
import com.tasktracker.task.repository.SprintRepository;
import com.tasktracker.task.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BoardService {

    private final BoardRepository repository;
    private final BoardColumnRepository columnRepository;
    private final ProjectRepository projectRepository;
    private final BoardMemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final BoardAccessService accessService;
    private final TaskRealtimePublisher realtimePublisher;

    public BoardService(BoardRepository repository,
                        BoardColumnRepository columnRepository,
                        ProjectRepository projectRepository,
                        BoardMemberRepository memberRepository,
                        ProjectMemberRepository projectMemberRepository,
                        SprintRepository sprintRepository,
                        TaskRepository taskRepository,
                        BoardAccessService accessService,
                        TaskRealtimePublisher realtimePublisher) {
        this.repository = repository;
        this.columnRepository = columnRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.accessService = accessService;
        this.realtimePublisher = realtimePublisher;
    }

    public Board create(BoardRequest request, String creatorEmail) {
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new AppException("Project not found", HttpStatus.NOT_FOUND));
        boolean isOwner = project.getOwnerEmail().equalsIgnoreCase(creatorEmail);
        if (!isOwner && projectMemberRepository.findByProjectIdAndUserEmail(request.projectId(), creatorEmail).isEmpty()) {
            throw new AppException("Project access denied", HttpStatus.FORBIDDEN);
        }
        if (!isOwner && projectMemberRepository.findByProjectIdAndUserEmail(request.projectId(), creatorEmail)
                .map(member -> member.getRole() == com.tasktracker.task.entity.ProjectRole.VIEWER)
                .orElse(false)) {
            throw new AppException("Viewers cannot create boards", HttpStatus.FORBIDDEN);
        }
        if (!projectRepository.existsById(request.projectId())) {
            throw new AppException("Project not found", HttpStatus.NOT_FOUND);
        }
        BoardType type = request.type() != null ? request.type() : BoardType.STANDARD;
        BoardMethodology methodology = type == BoardType.SPRINT && request.methodology() != null
                ? request.methodology()
                : type == BoardType.SPRINT
                    ? BoardMethodology.SCRUM
                    : BoardMethodology.KANBAN;
        if (type == BoardType.SPRINT && (request.sprintStartDate() == null || request.sprintEndDate() == null)) {
            throw new AppException("Sprint period is required", HttpStatus.BAD_REQUEST);
        }
        if (type == BoardType.SPRINT && request.sprintEndDate().isBefore(request.sprintStartDate())) {
            throw new AppException("Sprint end date must be after start date", HttpStatus.BAD_REQUEST);
        }

        Board board = new Board();
        board.setProjectId(request.projectId());
        board.setName(request.name());
        board.setType(type);
        board.setMethodology(methodology);
        board.setCreatedBy(creatorEmail);
        Board saved = repository.save(board);

        if (columnRepository.findByBoardIdOrderByPositionAsc(saved.getId()).isEmpty()) {
            createDefaultColumns(saved.getId());
        }

        addBoardOwner(saved.getId(), creatorEmail);

        if (!isOwner) {
            memberRepository.findByBoardIdAndUserEmail(saved.getId(), project.getOwnerEmail())
                    .orElseGet(() -> {
                        BoardMember lead = new BoardMember();
                        lead.setBoardId(saved.getId());
                        lead.setUserEmail(project.getOwnerEmail());
                        lead.setRole(BoardRole.OWNER);
                        return memberRepository.save(lead);
                    });
        }

        if (type == BoardType.SPRINT) {
            ensureProjectBacklog(request.projectId(), request.name(), creatorEmail, isOwner ? null : project.getOwnerEmail());
            Sprint sprint = createInitialSprint(saved, request);
            moveSelectedBacklogTasksToSprint(saved, sprint, request.backlogTaskIds(), creatorEmail);
        }

        publish(saved, RealtimeAction.CREATED, creatorEmail);
        return saved;
    }

    private void ensureProjectBacklog(UUID projectId, String sprintBoardName, String creatorEmail, String projectOwnerEmail) {
        if (repository.existsByProjectIdAndType(projectId, BoardType.BACKLOG)) {
            return;
        }
        Board backlog = new Board();
        backlog.setProjectId(projectId);
        backlog.setName("Backlog (" + sprintBoardName + ")");
        backlog.setType(BoardType.BACKLOG);
        backlog.setMethodology(BoardMethodology.KANBAN);
        backlog.setCreatedBy(creatorEmail);
        Board saved = repository.save(backlog);
        createDefaultColumns(saved.getId());
        addBoardOwner(saved.getId(), creatorEmail);
        if (projectOwnerEmail != null && !projectOwnerEmail.equalsIgnoreCase(creatorEmail)) {
            addBoardOwner(saved.getId(), projectOwnerEmail);
        }
        publish(saved, RealtimeAction.CREATED, creatorEmail);
    }

    private Sprint createInitialSprint(Board board, BoardRequest request) {
        Sprint sprint = new Sprint();
        sprint.setBoardId(board.getId());
        sprint.setName(request.name());
        sprint.setStartDate(request.sprintStartDate());
        sprint.setEndDate(request.sprintEndDate());
        sprint.setActive(true);
        sprint.setStatus(SprintStatus.ACTIVE);
        return sprintRepository.save(sprint);
    }

    private void moveSelectedBacklogTasksToSprint(Board sprintBoard, Sprint sprint, List<UUID> taskIds, String actorEmail) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        UUID firstColumnId = columnRepository.findByBoardIdOrderByPositionAsc(sprintBoard.getId()).stream()
                .findFirst()
                .map(BoardColumn::getId)
                .orElse(null);
        for (UUID taskId : taskIds) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new AppException("Backlog task not found", HttpStatus.NOT_FOUND));
            if (task.getBoardId() == null) {
                throw new AppException("Backlog task board not found", HttpStatus.CONFLICT);
            }
            Board sourceBoard = repository.findById(task.getBoardId())
                    .orElseThrow(() -> new AppException("Backlog task board not found", HttpStatus.NOT_FOUND));
            if (sourceBoard.getType() != BoardType.BACKLOG || !sourceBoard.getProjectId().equals(sprintBoard.getProjectId())) {
                throw new AppException("Only project backlog tasks can be moved to sprint", HttpStatus.CONFLICT);
            }
            task.setProjectId(sprintBoard.getProjectId());
            task.setBoardId(sprintBoard.getId());
            task.setSprintId(sprint.getId());
            task.setColumnId(firstColumnId);
            task.setBacklog(false);
            Task saved = taskRepository.save(task);
            realtimePublisher.publish(TaskRealtimeEvent.of(
                    RealtimeResource.TASK,
                    RealtimeAction.UPDATED,
                    sprintBoard.getProjectId(),
                    sprintBoard.getId(),
                    saved.getId(),
                    saved.getId(),
                    actorEmail
            ));
        }
    }

    private void createDefaultColumns(UUID boardId) {
        String[] names = {"К работе", "В работе", "Готово"};
        for (int i = 0; i < names.length; i++) {
            BoardColumn column = new BoardColumn();
            column.setBoardId(boardId);
            column.setName(names[i]);
            column.setPosition(i);
            columnRepository.save(column);
        }
    }

    public Board get(UUID id, String requesterEmail) {
        Board board = repository.findById(id)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        boolean isOwner = projectRepository.findById(board.getProjectId())
                .map(project -> project.getOwnerEmail().equalsIgnoreCase(requesterEmail))
                .orElse(false);
        boolean isMember = memberRepository.findByBoardIdAndUserEmail(id, requesterEmail).isPresent();
        if (!isOwner && !isMember) {
            throw new AppException("Board access denied", HttpStatus.FORBIDDEN);
        }
        return board;
    }

    public List<Board> listByProject(UUID projectId, BoardType type, String requesterEmail) {
        boolean isOwner = projectRepository.findById(projectId)
                .map(project -> project.getOwnerEmail().equalsIgnoreCase(requesterEmail))
                .orElse(false);
        if (!isOwner && projectMemberRepository.findByProjectIdAndUserEmail(projectId, requesterEmail).isEmpty()) {
            throw new AppException("Project access denied", HttpStatus.FORBIDDEN);
        }
        if (isOwner) {
            return type == null
                    ? repository.findByProjectId(projectId)
                    : repository.findByProjectIdAndType(projectId, type);
        }
        List<UUID> boardIds = memberRepository.findByUserEmail(requesterEmail).stream()
                .map(BoardMember::getBoardId)
                .toList();
        if (boardIds.isEmpty()) {
            return List.of();
        }
        return type == null
                ? repository.findByProjectIdAndIdIn(projectId, boardIds)
                : repository.findByProjectIdAndTypeAndIdIn(projectId, type, boardIds);
    }

    public Board update(UUID id, BoardUpdateRequest request, String requesterEmail) {
        Board board = repository.findById(id)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        accessService.requireBoardOwner(id, requesterEmail);
        if (request.name() != null && !request.name().isBlank()) {
            board.setName(request.name());
        }
        if (request.type() != null) {
            board.setType(request.type());
        }
        if (request.methodology() != null) {
            board.setMethodology(board.getType() == BoardType.SPRINT ? request.methodology() : BoardMethodology.KANBAN);
        }
        Board saved = repository.save(board);
        publish(saved, RealtimeAction.UPDATED, requesterEmail);
        return saved;
    }

    private void addBoardOwner(UUID boardId, String email) {
        memberRepository.findByBoardIdAndUserEmail(boardId, email)
                .orElseGet(() -> {
                    BoardMember owner = new BoardMember();
                    owner.setBoardId(boardId);
                    owner.setUserEmail(email);
                    owner.setRole(BoardRole.OWNER);
                    return memberRepository.save(owner);
                });
    }

    public void delete(UUID id, UUID targetBoardId, String requesterEmail) {
        Board board = repository.findById(id)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        accessService.requireBoardOwner(id, requesterEmail);
        UUID destinationBoardId = resolveDestinationBoard(board, targetBoardId, requesterEmail);
        moveBoardTasksBeforeDelete(board, destinationBoardId, requesterEmail);
        repository.delete(board);
        publish(board, RealtimeAction.DELETED, requesterEmail);
    }

    private UUID resolveDestinationBoard(Board deletedBoard, UUID targetBoardId, String requesterEmail) {
        if (targetBoardId != null) {
            if (targetBoardId.equals(deletedBoard.getId())) {
                throw new AppException("Target board must be different", HttpStatus.CONFLICT);
            }
            Board targetBoard = repository.findById(targetBoardId)
                    .orElseThrow(() -> new AppException("Target board not found", HttpStatus.NOT_FOUND));
            if (!targetBoard.getProjectId().equals(deletedBoard.getProjectId())) {
                throw new AppException("Target board must belong to the same project", HttpStatus.CONFLICT);
            }
            accessService.requireBoardOwner(targetBoardId, requesterEmail);
            return targetBoardId;
        }
        List<Board> remainingBoards = repository.findByProjectIdAndIdNot(deletedBoard.getProjectId(), deletedBoard.getId());
        if (remainingBoards.isEmpty()) {
            return null;
        }
        if (remainingBoards.size() == 1) {
            return remainingBoards.get(0).getId();
        }
        throw new AppException("Target board is required when project has multiple boards", HttpStatus.BAD_REQUEST);
    }

    private void moveBoardTasksBeforeDelete(Board deletedBoard, UUID destinationBoardId, String requesterEmail) {
        List<Task> tasks = taskRepository.findByBoardId(deletedBoard.getId());
        for (Task task : tasks) {
            task.setBoardId(destinationBoardId);
            task.setColumnId(null);
            task.setSprintId(null);
            task.setBacklog(true);
            Task saved = taskRepository.save(task);
            realtimePublisher.publish(TaskRealtimeEvent.of(
                    RealtimeResource.TASK,
                    RealtimeAction.UPDATED,
                    deletedBoard.getProjectId(),
                    destinationBoardId,
                    saved.getId(),
                    saved.getId(),
                    requesterEmail
            ));
        }
    }

    private void publish(Board board, RealtimeAction action, String actorEmail) {
        realtimePublisher.publish(TaskRealtimeEvent.of(
                RealtimeResource.BOARD,
                action,
                board.getProjectId(),
                board.getId(),
                null,
                board.getId(),
                actorEmail
        ));
    }
}
