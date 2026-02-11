package com.tasktracker.task.service;

import com.tasktracker.task.entity.BoardInvite;
import com.tasktracker.task.entity.BoardMember;
import com.tasktracker.task.entity.BoardRole;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.repository.BoardInviteRepository;
import com.tasktracker.task.repository.BoardMemberRepository;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.ProjectMemberRepository;
import com.tasktracker.task.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BoardInviteService {

    private final BoardInviteRepository repository;
    private final BoardRepository boardRepository;
    private final BoardMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public BoardInviteService(BoardInviteRepository repository,
                              BoardRepository boardRepository,
                              BoardMemberRepository memberRepository,
                              ProjectRepository projectRepository,
                              ProjectMemberRepository projectMemberRepository) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public BoardInvite createInvite(UUID boardId, String creatorEmail) {
        var board = boardRepository.findById(boardId)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        boolean isOwner = projectRepository.findById(board.getProjectId())
                .map(project -> project.getOwnerEmail().equalsIgnoreCase(creatorEmail))
                .orElse(false);
        boolean isBoardOwner = memberRepository.findByBoardIdAndUserEmail(boardId, creatorEmail)
                .map(member -> member.getRole() == BoardRole.OWNER)
                .orElse(false);
        if (!isOwner && !isBoardOwner) {
            throw new AppException("Board access denied", HttpStatus.FORBIDDEN);
        }
        BoardInvite invite = new BoardInvite();
        invite.setBoardId(boardId);
        invite.setCreatedBy(creatorEmail);
        invite.setToken(UUID.randomUUID().toString());
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        invite.setActive(true);
        return repository.save(invite);
    }

    public BoardInvite acceptInvite(String token, String userEmail) {
        BoardInvite invite = repository.findByToken(token)
                .orElseThrow(() -> new AppException("Invite not found", HttpStatus.NOT_FOUND));
        if (!invite.isActive()) {
            throw new AppException("Invite is inactive", HttpStatus.CONFLICT);
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException("Invite expired", HttpStatus.CONFLICT);
        }
        var board = boardRepository.findById(invite.getBoardId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        boolean isProjectOwner = projectRepository.findById(board.getProjectId())
                .map(project -> project.getOwnerEmail().equalsIgnoreCase(userEmail))
                .orElse(false);
        if (!isProjectOwner && projectMemberRepository.findByProjectIdAndUserEmail(board.getProjectId(), userEmail).isEmpty()) {
            throw new AppException("User is not a project member", HttpStatus.CONFLICT);
        }
        memberRepository.findByBoardIdAndUserEmail(invite.getBoardId(), userEmail)
                .orElseGet(() -> {
                    BoardMember member = new BoardMember();
                    member.setBoardId(invite.getBoardId());
                    member.setUserEmail(userEmail);
                    member.setRole(BoardRole.VIEWER);
                    return memberRepository.save(member);
                });
        return invite;
    }
}
