package com.tasktracker.task.service;

import com.tasktracker.task.dto.SprintRequest;
import com.tasktracker.task.entity.Sprint;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.SprintRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SprintService {

    private final SprintRepository repository;
    private final BoardRepository boardRepository;
    private final BoardAccessService accessService;

    public SprintService(SprintRepository repository,
                         BoardRepository boardRepository,
                         BoardAccessService accessService) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.accessService = accessService;
    }

    public Sprint create(SprintRequest request, String requesterEmail) {
        accessService.requireBoardOwner(request.boardId(), requesterEmail);
        if (!boardRepository.existsById(request.boardId())) {
            throw new AppException("Board not found", HttpStatus.NOT_FOUND);
        }
        Sprint sprint = new Sprint();
        sprint.setBoardId(request.boardId());
        sprint.setName(request.name());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        if (request.active() != null) {
            sprint.setActive(request.active());
        }
        return repository.save(sprint);
    }

    public List<Sprint> list(UUID boardId, String requesterEmail) {
        accessService.requireBoardAccess(boardId, requesterEmail);
        return repository.findByBoardId(boardId);
    }
}
