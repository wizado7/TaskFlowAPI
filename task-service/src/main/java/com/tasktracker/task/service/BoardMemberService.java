package com.tasktracker.task.service;

import com.tasktracker.task.dto.BoardMemberRequest;
import com.tasktracker.task.entity.BoardMember;
import com.tasktracker.task.entity.BoardRole;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.realtime.RealtimeAction;
import com.tasktracker.task.realtime.RealtimeResource;
import com.tasktracker.task.realtime.TaskRealtimeEvent;
import com.tasktracker.task.realtime.TaskRealtimePublisher;
import com.tasktracker.task.repository.BoardMemberRepository;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.ProjectMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BoardMemberService {

    private final BoardMemberRepository repository;
    private final BoardRepository boardRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final BoardAccessService accessService;
    private final TaskRealtimePublisher realtimePublisher;

    public BoardMemberService(BoardMemberRepository repository,
                              BoardRepository boardRepository,
                              ProjectMemberRepository projectMemberRepository,
                              ProjectService projectService,
                              BoardAccessService accessService,
                              TaskRealtimePublisher realtimePublisher) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.accessService = accessService;
        this.realtimePublisher = realtimePublisher;
    }

    public BoardMember addMember(UUID boardId, BoardMemberRequest request, String requesterEmail) {
        var board = boardRepository.findById(boardId)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        requireBoardCreator(board, requesterEmail);
        if (!projectService.isOwner(board.getProjectId(), request.email()) &&
                projectMemberRepository.findByProjectIdAndUserEmail(board.getProjectId(), request.email()).isEmpty()) {
            throw new AppException("User is not a project member", HttpStatus.CONFLICT);
        }
        BoardMember member = repository.findByBoardIdAndUserEmail(boardId, request.email())
                .orElseGet(() -> {
                    BoardMember newMember = new BoardMember();
                    newMember.setBoardId(boardId);
                    newMember.setUserEmail(request.email());
                    newMember.setRole(request.role() != null ? request.role() : BoardRole.VIEWER);
                    return repository.save(newMember);
                });
        publish(board, member.getId(), RealtimeAction.CREATED, requesterEmail);
        return member;
    }

    public List<BoardMember> listMembers(UUID boardId, String requesterEmail) {
        accessService.requireBoardAccess(boardId, requesterEmail);
        return repository.findByBoardId(boardId);
    }

    public BoardMember updateRole(UUID memberId, BoardRole role, String requesterEmail) {
        BoardMember member = repository.findById(memberId)
                .orElseThrow(() -> new AppException("Member not found", HttpStatus.NOT_FOUND));
        var board = boardRepository.findById(member.getBoardId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        requireBoardCreator(board, requesterEmail);
        member.setRole(role != null ? role : BoardRole.VIEWER);
        BoardMember saved = repository.save(member);
        publish(board, saved.getId(), RealtimeAction.UPDATED, requesterEmail);
        return saved;
    }

    public void removeMember(UUID memberId, String requesterEmail) {
        BoardMember member = repository.findById(memberId)
                .orElseThrow(() -> new AppException("Member not found", HttpStatus.NOT_FOUND));
        var board = boardRepository.findById(member.getBoardId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        requireBoardCreator(board, requesterEmail);
        repository.deleteById(memberId);
        publish(board, memberId, RealtimeAction.DELETED, requesterEmail);
    }

    private void requireBoardCreator(com.tasktracker.task.entity.Board board, String requesterEmail) {
        if (!board.getCreatedBy().equalsIgnoreCase(requesterEmail)) {
            throw new AppException("Only board creator can manage board access", HttpStatus.FORBIDDEN);
        }
    }

    private void publish(com.tasktracker.task.entity.Board board, UUID memberId, RealtimeAction action, String actorEmail) {
        realtimePublisher.publish(TaskRealtimeEvent.of(
                RealtimeResource.BOARD_MEMBER,
                action,
                board.getProjectId(),
                board.getId(),
                null,
                memberId,
                actorEmail
        ));
    }
}
