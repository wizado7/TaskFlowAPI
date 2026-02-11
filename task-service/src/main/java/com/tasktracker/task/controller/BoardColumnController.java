package com.tasktracker.task.controller;

import com.tasktracker.task.dto.BoardColumnRequest;
import com.tasktracker.task.dto.BoardColumnResponse;
import com.tasktracker.task.dto.BoardColumnUpdateRequest;
import com.tasktracker.task.entity.BoardColumn;
import com.tasktracker.task.service.BoardColumnService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BoardColumnController {

    private final BoardColumnService service;

    public BoardColumnController(BoardColumnService service) {
        this.service = service;
    }

    @PostMapping("/boards/{boardId}/columns")
    public ResponseEntity<BoardColumnResponse> create(@PathVariable("boardId") UUID boardId,
                                                      @AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody BoardColumnRequest request) {
        BoardColumn column = service.create(boardId, request, jwt.getSubject());
        return ResponseEntity.ok(toResponse(column));
    }

    @GetMapping("/boards/{boardId}/columns")
    public ResponseEntity<List<BoardColumnResponse>> list(@PathVariable("boardId") UUID boardId,
                                                          @AuthenticationPrincipal Jwt jwt) {
        var columns = service.list(boardId, jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(columns);
    }

    @PutMapping("/columns/{id}")
    public ResponseEntity<BoardColumnResponse> update(@PathVariable("id") UUID id,
                                                      @AuthenticationPrincipal Jwt jwt,
                                                      @RequestBody BoardColumnUpdateRequest request) {
        BoardColumn column = service.update(id, request, jwt.getSubject());
        return ResponseEntity.ok(toResponse(column));
    }

    @DeleteMapping("/columns/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id,
                                       @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    private BoardColumnResponse toResponse(BoardColumn column) {
        return new BoardColumnResponse(
                column.getId(),
                column.getBoardId(),
                column.getName(),
                column.getPosition()
        );
    }
}
