package com.tasktracker.task.service;

import com.tasktracker.task.dto.BoardMemberRequest;
import com.tasktracker.task.entity.BoardMember;
import com.tasktracker.task.entity.BoardRole;
import com.tasktracker.task.exception.AppException;
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

    public BoardMemberService(BoardMemberRepository repository,
                              BoardRepository boardRepository,
                              ProjectMemberRepository projectMemberRepository,
                              ProjectService projectService,
                              BoardAccessService accessService) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.accessService = accessService;
    }

    public BoardMember addMember(UUID boardId, BoardMemberRequest request, String requesterEmail) {
        var board = boardRepository.findById(boardId)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        requireBoardCreator(board, requesterEmail);
        if (!projectService.isOwner(board.getProjectId(), request.email()) &&
                projectMemberRepository.findByProjectIdAndUserEmail(board.getProjectId(), request.email()).isEmpty()) {
            throw new AppException("User is not a project member", HttpStatus.CONFLICT);
        }
        return repository.findByBoardIdAndUserEmail(boardId, request.email())
                .orElseGet(() -> {
                    BoardMember member = new BoardMember();
                    member.setBoardId(boardId);
                    member.setUserEmail(request.email());
                    member.setRole(request.role() != null ? request.role() : BoardRole.VIEWER);
                    return repository.save(member);
                });
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
        return repository.save(member);
    }

    public void removeMember(UUID memberId, String requesterEmail) {
        BoardMember member = repository.findById(memberId)
                .orElseThrow(() -> new AppException("Member not found", HttpStatus.NOT_FOUND));
        var board = boardRepository.findById(member.getBoardId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        requireBoardCreator(board, requesterEmail);
        repository.deleteById(memberId);
    }

    private void requireBoardCreator(com.tasktracker.task.entity.Board board, String requesterEmail) {
        if (!board.getCreatedBy().equalsIgnoreCase(requesterEmail)) {
            throw new AppException("Only board creator can manage board access", HttpStatus.FORBIDDEN);
        }
    }
}