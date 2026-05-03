package com.tasktracker.task.service;

import com.tasktracker.task.dto.BoardColumnRequest;
import com.tasktracker.task.dto.BoardColumnUpdateRequest;
import com.tasktracker.task.entity.Board;
import com.tasktracker.task.entity.BoardColumn;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.realtime.RealtimeAction;
import com.tasktracker.task.realtime.RealtimeResource;
import com.tasktracker.task.realtime.TaskRealtimeEvent;
import com.tasktracker.task.realtime.TaskRealtimePublisher;
import com.tasktracker.task.repository.BoardColumnRepository;
import com.tasktracker.task.repository.BoardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BoardColumnService {

    private final BoardColumnRepository repository;
    private final BoardRepository boardRepository;
    private final BoardAccessService accessService;
    private final TaskRealtimePublisher realtimePublisher;

    public BoardColumnService(BoardColumnRepository repository,
                              BoardRepository boardRepository,
                              BoardAccessService accessService,
                              TaskRealtimePublisher realtimePublisher) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.accessService = accessService;
        this.realtimePublisher = realtimePublisher;
    }

    public BoardColumn create(UUID boardId, BoardColumnRequest request, String requesterEmail) {
        accessService.requireBoardEditor(boardId, requesterEmail);
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        BoardColumn column = new BoardColumn();
        column.setBoardId(boardId);
        column.setName(request.name());
        column.setPosition(request.position());
        BoardColumn saved = repository.save(column);
        publish(board, saved.getId(), RealtimeAction.CREATED, requesterEmail);
        return saved;
    }

    public List<BoardColumn> list(UUID boardId, String requesterEmail) {
        accessService.requireBoardAccess(boardId, requesterEmail);
        return repository.findByBoardIdOrderByPositionAsc(boardId);
    }

    public BoardColumn update(UUID id, BoardColumnUpdateRequest request, String requesterEmail) {
        BoardColumn column = repository.findById(id)
                .orElseThrow(() -> new AppException("Column not found", HttpStatus.NOT_FOUND));
        accessService.requireBoardEditor(column.getBoardId(), requesterEmail);
        if (request.name() != null) {
            column.setName(request.name());
        }
        if (request.position() != null) {
            column.setPosition(request.position());
        }
        BoardColumn saved = repository.save(column);
        Board board = boardRepository.findById(saved.getBoardId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        publish(board, saved.getId(), RealtimeAction.UPDATED, requesterEmail);
        return saved;
    }

    public void delete(UUID id, String requesterEmail) {
        BoardColumn column = repository.findById(id)
                .orElseThrow(() -> new AppException("Column not found", HttpStatus.NOT_FOUND));
        accessService.requireBoardEditor(column.getBoardId(), requesterEmail);
        Board board = boardRepository.findById(column.getBoardId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        repository.deleteById(id);
        publish(board, id, RealtimeAction.DELETED, requesterEmail);
    }

    private void publish(Board board, UUID columnId, RealtimeAction action, String actorEmail) {
        realtimePublisher.publish(TaskRealtimeEvent.of(
                RealtimeResource.COLUMN,
                action,
                board.getProjectId(),
                board.getId(),
                null,
                columnId,
                actorEmail
        ));
    }
}
