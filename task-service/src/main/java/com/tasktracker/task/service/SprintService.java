package com.tasktracker.task.service;

import com.tasktracker.task.dto.SprintRequest;
import com.tasktracker.task.entity.BoardType;
import com.tasktracker.task.entity.Sprint;
import com.tasktracker.task.entity.SprintStatus;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.realtime.RealtimeAction;
import com.tasktracker.task.realtime.RealtimeResource;
import com.tasktracker.task.realtime.TaskRealtimeEvent;
import com.tasktracker.task.realtime.TaskRealtimePublisher;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.SprintRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SprintService {

    private final SprintRepository repository;
    private final BoardRepository boardRepository;
    private final BoardAccessService accessService;
    private final TaskService taskService;
    private final TaskRealtimePublisher realtimePublisher;

    public SprintService(SprintRepository repository,
                         BoardRepository boardRepository,
                         BoardAccessService accessService,
                         TaskService taskService,
                         TaskRealtimePublisher realtimePublisher) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.accessService = accessService;
        this.taskService = taskService;
        this.realtimePublisher = realtimePublisher;
    }

    public Sprint create(SprintRequest request, String requesterEmail) {
        accessService.requireBoardOwner(request.boardId(), requesterEmail);
        var board = boardRepository.findById(request.boardId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        if (board.getType() != BoardType.SPRINT) {
            throw new AppException("Sprints can be created only on Scrum boards", HttpStatus.CONFLICT);
        }
        if (Boolean.TRUE.equals(request.active()) && repository.existsByBoardIdAndStatus(request.boardId(), SprintStatus.ACTIVE)) {
            throw new AppException("Board already has active sprint", HttpStatus.CONFLICT);
        }
        Sprint sprint = new Sprint();
        sprint.setBoardId(request.boardId());
        sprint.setName(request.name());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        sprint.setGoal(request.goal());
        sprint.setCapacityPoints(request.capacityPoints());
        if (request.active() != null) {
            sprint.setActive(request.active());
            sprint.setStatus(request.active() ? SprintStatus.ACTIVE : SprintStatus.PLANNED);
        }
        Sprint saved = repository.save(sprint);
        publish(saved, RealtimeAction.CREATED, requesterEmail);
        return saved;
    }

    public List<Sprint> list(UUID boardId, String requesterEmail) {
        accessService.requireBoardAccess(boardId, requesterEmail);
        return repository.findByBoardId(boardId);
    }

    public Optional<Sprint> active(UUID boardId, String requesterEmail) {
        accessService.requireBoardAccess(boardId, requesterEmail);
        return repository.findByBoardIdAndStatus(boardId, SprintStatus.ACTIVE);
    }

    public Sprint start(UUID sprintId, String requesterEmail) {
        Sprint sprint = repository.findById(sprintId)
                .orElseThrow(() -> new AppException("Sprint not found", HttpStatus.NOT_FOUND));
        accessService.requireBoardOwner(sprint.getBoardId(), requesterEmail);
        if (sprint.getStatus() == SprintStatus.COMPLETED) {
            throw new AppException("Completed sprint cannot be started", HttpStatus.CONFLICT);
        }
        repository.findByBoardIdAndStatus(sprint.getBoardId(), SprintStatus.ACTIVE)
                .filter(activeSprint -> !activeSprint.getId().equals(sprintId))
                .ifPresent(activeSprint -> {
                    throw new AppException("Board already has active sprint", HttpStatus.CONFLICT);
                });
        sprint.setActive(true);
        sprint.setStatus(SprintStatus.ACTIVE);
        Sprint saved = repository.save(sprint);
        publish(saved, RealtimeAction.UPDATED, requesterEmail);
        return saved;
    }

    public Sprint complete(UUID sprintId, UUID targetBoardId, String requesterEmail) {
        Sprint sprint = repository.findById(sprintId)
                .orElseThrow(() -> new AppException("Sprint not found", HttpStatus.NOT_FOUND));
        accessService.requireBoardOwner(sprint.getBoardId(), requesterEmail);
        if (sprint.getStatus() == SprintStatus.COMPLETED) {
            return sprint;
        }
        if (targetBoardId != null) {
            var sourceBoard = boardRepository.findById(sprint.getBoardId())
                    .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
            var targetBoard = boardRepository.findById(targetBoardId)
                    .orElseThrow(() -> new AppException("Target board not found", HttpStatus.NOT_FOUND));
            if (!targetBoard.getProjectId().equals(sourceBoard.getProjectId())) {
                throw new AppException("Target board must belong to the same project", HttpStatus.CONFLICT);
            }
            accessService.requireBoardOwner(targetBoardId, requesterEmail);
        }
        taskService.moveUnfinishedSprintTasksToBacklog(sprintId, targetBoardId, requesterEmail);
        sprint.setActive(false);
        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(LocalDateTime.now());
        Sprint saved = repository.save(sprint);
        publish(saved, RealtimeAction.UPDATED, requesterEmail);
        return saved;
    }

    private void publish(Sprint sprint, RealtimeAction action, String actorEmail) {
        UUID projectId = boardRepository.findById(sprint.getBoardId())
                .map(board -> board.getProjectId())
                .orElse(null);
        realtimePublisher.publish(TaskRealtimeEvent.of(
                RealtimeResource.SPRINT,
                action,
                projectId,
                sprint.getBoardId(),
                null,
                sprint.getId(),
                actorEmail
        ));
    }
}
