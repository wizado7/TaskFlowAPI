package com.tasktracker.task.service;

import com.tasktracker.task.entity.Board;
import com.tasktracker.task.entity.BoardMember;
import com.tasktracker.task.entity.BoardRole;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.repository.BoardMemberRepository;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.ProjectMemberRepository;
import com.tasktracker.task.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BoardAccessService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public BoardAccessService(BoardRepository boardRepository,
                              BoardMemberRepository boardMemberRepository,
                              ProjectRepository projectRepository,
                              ProjectMemberRepository projectMemberRepository) {
        this.boardRepository = boardRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public Board getBoard(UUID boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
    }

    public boolean isProjectOwner(UUID projectId, String userEmail) {
        return projectRepository.findById(projectId)
                .map(project -> project.getOwnerEmail().equalsIgnoreCase(userEmail))
                .orElse(false);
    }

    public boolean isProjectMember(UUID projectId, String userEmail) {
        if (isProjectOwner(projectId, userEmail)) {
            return true;
        }
        return projectMemberRepository.findByProjectIdAndUserEmail(projectId, userEmail).isPresent();
    }

    public void requireProjectMember(UUID projectId, String userEmail) {
        if (!isProjectMember(projectId, userEmail)) {
            throw new AppException("Project access denied", HttpStatus.FORBIDDEN);
        }
    }

    public boolean isBoardMember(UUID boardId, String userEmail) {
        return boardMemberRepository.findByBoardIdAndUserEmail(boardId, userEmail).isPresent();
    }

    public boolean isBoardOwner(UUID boardId, String userEmail) {
        return boardMemberRepository.findByBoardIdAndUserEmail(boardId, userEmail)
                .map(member -> member.getRole() == BoardRole.OWNER)
                .orElse(false);
    }

    public boolean isBoardEditor(UUID boardId, String userEmail) {
        return boardMemberRepository.findByBoardIdAndUserEmail(boardId, userEmail)
                .map(member -> member.getRole() == BoardRole.EDITOR)
                .orElse(false);
    }

    public boolean isBoardEditorOrOwner(UUID boardId, String userEmail) {
        return boardMemberRepository.findByBoardIdAndUserEmail(boardId, userEmail)
                .map(member -> member.getRole() == BoardRole.OWNER || member.getRole() == BoardRole.EDITOR)
                .orElse(false);
    }

    public void requireBoardAccess(UUID boardId, String userEmail) {
        Board board = getBoard(boardId);
        if (isProjectOwner(board.getProjectId(), userEmail) || isBoardMember(boardId, userEmail)) {
            return;
        }
        throw new AppException("Board access denied", HttpStatus.FORBIDDEN);
    }

    public void requireBoardOwner(UUID boardId, String userEmail) {
        Board board = getBoard(boardId);
        if (isProjectOwner(board.getProjectId(), userEmail) || isBoardOwner(boardId, userEmail)) {
            return;
        }
        throw new AppException("Board management denied", HttpStatus.FORBIDDEN);
    }

    public void requireBoardEditor(UUID boardId, String userEmail) {
        Board board = getBoard(boardId);
        if (isProjectOwner(board.getProjectId(), userEmail) || isBoardEditorOrOwner(boardId, userEmail)) {
            return;
        }
        throw new AppException("Board management denied", HttpStatus.FORBIDDEN);
    }

    public List<UUID> boardIdsForUser(String userEmail) {
        return boardMemberRepository.findByUserEmail(userEmail).stream()
                .map(BoardMember::getBoardId)
                .toList();
    }
}