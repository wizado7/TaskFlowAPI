package com.tasktracker.task.service;

import com.tasktracker.task.dto.BoardRequest;
import com.tasktracker.task.dto.BoardUpdateRequest;
import com.tasktracker.task.entity.Board;
import com.tasktracker.task.entity.BoardColumn;
import com.tasktracker.task.entity.BoardMember;
import com.tasktracker.task.entity.BoardRole;
import com.tasktracker.task.entity.BoardType;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.repository.BoardColumnRepository;
import com.tasktracker.task.repository.BoardMemberRepository;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.ProjectRepository;
import com.tasktracker.task.repository.ProjectMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BoardService {

    private final BoardRepository repository;
    private final BoardColumnRepository columnRepository;
    private final ProjectRepository projectRepository;
    private final BoardMemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardAccessService accessService;

    public BoardService(BoardRepository repository,
                        BoardColumnRepository columnRepository,
                        ProjectRepository projectRepository,
                        BoardMemberRepository memberRepository,
                        ProjectMemberRepository projectMemberRepository,
                        BoardAccessService accessService) {
        this.repository = repository;
        this.columnRepository = columnRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.accessService = accessService;
    }

    public Board create(BoardRequest request, String creatorEmail) {
        var project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new AppException("Project not found", HttpStatus.NOT_FOUND));
        boolean isOwner = project.getOwnerEmail().equalsIgnoreCase(creatorEmail);
        if (!isOwner && projectMemberRepository.findByProjectIdAndUserEmail(request.projectId(), creatorEmail).isEmpty()) {
            throw new AppException("Project access denied", HttpStatus.FORBIDDEN);
        }
        if (!isOwner && projectMemberRepository.findByProjectIdAndUserEmail(request.projectId(), creatorEmail)
                .map(member -> member.getRole() == com.tasktracker.task.entity.ProjectRole.VIEWER)
                .orElse(false)) {
            throw new AppException("Viewers cannot create boards", HttpStatus.FORBIDDEN);
        }
        if (!projectRepository.existsById(request.projectId())) {
            throw new AppException("Project not found", HttpStatus.NOT_FOUND);
        }
        Board board = new Board();
        board.setProjectId(request.projectId());
        board.setName(request.name());
        board.setType(request.type() != null ? request.type() : BoardType.STANDARD);
        board.setCreatedBy(creatorEmail);
        Board saved = repository.save(board);

        if (columnRepository.findByBoardIdOrderByPositionAsc(saved.getId()).isEmpty()) {
            createDefaultColumns(saved.getId());
        }

        BoardMember owner = new BoardMember();
        owner.setBoardId(saved.getId());
        owner.setUserEmail(creatorEmail);
        owner.setRole(BoardRole.OWNER);
        memberRepository.save(owner);

        if (!isOwner) {
            memberRepository.findByBoardIdAndUserEmail(saved.getId(), project.getOwnerEmail())
                    .orElseGet(() -> {
                        BoardMember lead = new BoardMember();
                        lead.setBoardId(saved.getId());
                        lead.setUserEmail(project.getOwnerEmail());
                        lead.setRole(BoardRole.OWNER);
                        return memberRepository.save(lead);
                    });
        }

        return saved;
    }

    private void createDefaultColumns(UUID boardId) {
        String[] names = {"К работе", "В работе", "Готово"};
        for (int i = 0; i < names.length; i++) {
            BoardColumn column = new BoardColumn();
            column.setBoardId(boardId);
            column.setName(names[i]);
            column.setPosition(i);
            columnRepository.save(column);
        }
    }

    public Board get(UUID id, String requesterEmail) {
        Board board = repository.findById(id)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        boolean isOwner = projectRepository.findById(board.getProjectId())
                .map(project -> project.getOwnerEmail().equalsIgnoreCase(requesterEmail))
                .orElse(false);
        boolean isMember = memberRepository.findByBoardIdAndUserEmail(id, requesterEmail).isPresent();
        if (!isOwner && !isMember) {
            throw new AppException("Board access denied", HttpStatus.FORBIDDEN);
        }
        return board;
    }

    public List<Board> listByProject(UUID projectId, BoardType type, String requesterEmail) {
        boolean isOwner = projectRepository.findById(projectId)
                .map(project -> project.getOwnerEmail().equalsIgnoreCase(requesterEmail))
                .orElse(false);
        if (!isOwner && projectMemberRepository.findByProjectIdAndUserEmail(projectId, requesterEmail).isEmpty()) {
            throw new AppException("Project access denied", HttpStatus.FORBIDDEN);
        }
        if (isOwner) {
            return type == null
                    ? repository.findByProjectId(projectId)
                    : repository.findByProjectIdAndType(projectId, type);
        }
        List<UUID> boardIds = memberRepository.findByUserEmail(requesterEmail).stream()
                .map(BoardMember::getBoardId)
                .toList();
        if (boardIds.isEmpty()) {
            return List.of();
        }
        return type == null
                ? repository.findByProjectIdAndIdIn(projectId, boardIds)
                : repository.findByProjectIdAndTypeAndIdIn(projectId, type, boardIds);
    }

    public Board update(UUID id, BoardUpdateRequest request, String requesterEmail) {
        Board board = repository.findById(id)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        accessService.requireBoardOwner(id, requesterEmail);
        if (request.name() != null && !request.name().isBlank()) {
            board.setName(request.name());
        }
        if (request.type() != null) {
            board.setType(request.type());
        }
        return repository.save(board);
    }

    public void delete(UUID id, String requesterEmail) {
        Board board = repository.findById(id)
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        accessService.requireBoardOwner(id, requesterEmail);
        repository.delete(board);
    }
}
