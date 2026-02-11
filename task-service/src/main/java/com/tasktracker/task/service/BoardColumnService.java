package com.tasktracker.task.service;

import com.tasktracker.task.dto.BoardColumnRequest;
import com.tasktracker.task.dto.BoardColumnUpdateRequest;
import com.tasktracker.task.entity.BoardColumn;
import com.tasktracker.task.exception.AppException;
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

    public BoardColumnService(BoardColumnRepository repository,
                              BoardRepository boardRepository,
                              BoardAccessService accessService) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.accessService = accessService;
    }

    public BoardColumn create(UUID boardId, BoardColumnRequest request, String requesterEmail) {
        accessService.requireBoardEditor(boardId, requesterEmail);
        if (!boardRepository.existsById(boardId)) {
            throw new AppException("Board not found", HttpStatus.NOT_FOUND);
        }
        BoardColumn column = new BoardColumn();
        column.setBoardId(boardId);
        column.setName(request.name());
        column.setPosition(request.position());
        return repository.save(column);
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
        return repository.save(column);
    }

    public void delete(UUID id, String requesterEmail) {
        if (!repository.existsById(id)) {
            throw new AppException("Column not found", HttpStatus.NOT_FOUND);
        }
        BoardColumn column = repository.findById(id)
                .orElseThrow(() -> new AppException("Column not found", HttpStatus.NOT_FOUND));
        accessService.requireBoardEditor(column.getBoardId(), requesterEmail);
        repository.deleteById(id);
    }
}
