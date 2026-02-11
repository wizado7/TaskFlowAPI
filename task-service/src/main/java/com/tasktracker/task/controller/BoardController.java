package com.tasktracker.task.controller;

import com.tasktracker.task.dto.BoardInviteResponse;
import com.tasktracker.task.dto.BoardMemberRequest;
import com.tasktracker.task.dto.BoardMemberResponse;
import com.tasktracker.task.dto.BoardRequest;
import com.tasktracker.task.dto.BoardResponse;
import com.tasktracker.task.dto.BoardUpdateRequest;
import com.tasktracker.task.dto.SprintRequest;
import com.tasktracker.task.dto.SprintResponse;
import com.tasktracker.task.entity.BoardType;
import com.tasktracker.task.service.BoardInviteService;
import com.tasktracker.task.service.BoardMemberService;
import com.tasktracker.task.service.BoardService;
import com.tasktracker.task.service.SprintService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/boards")
public class BoardController {

    private final BoardService boardService;
    private final SprintService sprintService;
    private final BoardInviteService inviteService;
    private final BoardMemberService memberService;

    public BoardController(BoardService boardService,
                           SprintService sprintService,
                           BoardInviteService inviteService,
                           BoardMemberService memberService) {
        this.boardService = boardService;
        this.sprintService = sprintService;
        this.inviteService = inviteService;
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<BoardResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody BoardRequest request) {
        var board = boardService.create(request, jwt.getSubject());
        return ResponseEntity.ok(toResponse(board));
    }

    @GetMapping
    public ResponseEntity<List<BoardResponse>> list(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam("projectId") UUID projectId,
                                                    @RequestParam(value = "type", required = false) BoardType type) {
        var boards = boardService.listByProject(projectId, type, jwt.getSubject()).stream()
                .map(this::toResponse).toList();
        return ResponseEntity.ok(boards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> get(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable("id") UUID id) {
        return ResponseEntity.ok(toResponse(boardService.get(id, jwt.getSubject())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoardResponse> update(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable("id") UUID id,
                                                @RequestBody BoardUpdateRequest request) {
        var board = boardService.update(id, request, jwt.getSubject());
        return ResponseEntity.ok(toResponse(board));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable("id") UUID id) {
        boardService.delete(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/sprints")
    public ResponseEntity<SprintResponse> createSprint(@PathVariable("id") UUID boardId,
                                                       @AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody SprintRequest request) {
        var sprint = sprintService.create(new SprintRequest(boardId, request.name(), request.startDate(), request.endDate(), request.active()),
                jwt.getSubject());
        return ResponseEntity.ok(toResponse(sprint));
    }

    @GetMapping("/{id}/sprints")
    public ResponseEntity<List<SprintResponse>> listSprints(@PathVariable("id") UUID boardId,
                                                            @AuthenticationPrincipal Jwt jwt) {
        var sprints = sprintService.list(boardId, jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(sprints);
    }

    @PostMapping("/{id}/invites")
    public ResponseEntity<BoardInviteResponse> createInvite(@PathVariable("id") UUID boardId,
                                                            @AuthenticationPrincipal Jwt jwt) {
        var invite = inviteService.createInvite(boardId, jwt.getSubject());
        return ResponseEntity.ok(toResponse(invite));
    }

    @PostMapping("/invites/{token}/accept")
    public ResponseEntity<BoardInviteResponse> acceptInvite(@PathVariable("token") String token,
                                                            @AuthenticationPrincipal Jwt jwt) {
        var invite = inviteService.acceptInvite(token, jwt.getSubject());
        return ResponseEntity.ok(toResponse(invite));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<BoardMemberResponse> addMember(@PathVariable("id") UUID boardId,
                                                         @AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody BoardMemberRequest request) {
        var member = memberService.addMember(boardId, request, jwt.getSubject());
        return ResponseEntity.ok(toResponse(member));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<BoardMemberResponse>> listMembers(@PathVariable("id") UUID boardId,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        var members = memberService.listMembers(boardId, jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(members);
    }

    @PutMapping("/members/{memberId}/role")
    public ResponseEntity<BoardMemberResponse> updateMemberRole(@PathVariable("memberId") UUID memberId,
                                                                 @AuthenticationPrincipal Jwt jwt,
                                                                 @RequestBody com.tasktracker.task.entity.BoardRole role) {
        var member = memberService.updateRole(memberId, role, jwt.getSubject());
        return ResponseEntity.ok(toResponse(member));
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable("memberId") UUID memberId,
                                             @AuthenticationPrincipal Jwt jwt) {
        memberService.removeMember(memberId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    private BoardResponse toResponse(com.tasktracker.task.entity.Board board) {
        return new BoardResponse(board.getId(), board.getProjectId(), board.getName(), board.getCreatedBy(), board.getType());
    }

    private SprintResponse toResponse(com.tasktracker.task.entity.Sprint sprint) {
        return new SprintResponse(sprint.getId(), sprint.getBoardId(), sprint.getName(), sprint.getStartDate(), sprint.getEndDate(), sprint.isActive());
    }

    private BoardInviteResponse toResponse(com.tasktracker.task.entity.BoardInvite invite) {
        return new BoardInviteResponse(invite.getId(), invite.getBoardId(), invite.getToken(), invite.getCreatedBy(), invite.getExpiresAt(), invite.isActive());
    }

    private BoardMemberResponse toResponse(com.tasktracker.task.entity.BoardMember member) {
        return new BoardMemberResponse(member.getId(), member.getBoardId(), member.getUserEmail(), member.getRole());
    }
}
