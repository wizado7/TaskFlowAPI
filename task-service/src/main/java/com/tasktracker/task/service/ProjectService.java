package com.tasktracker.task.service;

import com.tasktracker.task.dto.ProjectRequest;
import com.tasktracker.task.dto.ProjectUpdateRequest;
import com.tasktracker.task.entity.Project;
import com.tasktracker.task.entity.ProjectMember;
import com.tasktracker.task.entity.ProjectRole;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.repository.BoardColumnRepository;
import com.tasktracker.task.repository.BoardInviteRepository;
import com.tasktracker.task.repository.BoardMemberRepository;
import com.tasktracker.task.repository.BoardRepository;
import com.tasktracker.task.repository.ProjectMemberRepository;
import com.tasktracker.task.repository.ProjectInviteRepository;
import com.tasktracker.task.repository.ProjectRepository;
import com.tasktracker.task.repository.SprintRepository;
import com.tasktracker.task.repository.TaskCommentRepository;
import com.tasktracker.task.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository repository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectInviteRepository inviteRepository;
    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final BoardInviteRepository boardInviteRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;

    public ProjectService(ProjectRepository repository,
                          ProjectMemberRepository memberRepository,
                          ProjectInviteRepository inviteRepository,
                          BoardRepository boardRepository,
                          BoardMemberRepository boardMemberRepository,
                          BoardInviteRepository boardInviteRepository,
                          BoardColumnRepository boardColumnRepository,
                          SprintRepository sprintRepository,
                          TaskRepository taskRepository,
                          TaskCommentRepository taskCommentRepository) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.inviteRepository = inviteRepository;
        this.boardRepository = boardRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.boardInviteRepository = boardInviteRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.taskCommentRepository = taskCommentRepository;
    }

    public Project create(ProjectRequest request, String ownerEmail) {
        Project project = new Project();
        project.setName(request.name());
        project.setOwnerEmail(ownerEmail);
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        Project saved = repository.save(project);

        ProjectMember owner = new ProjectMember();
        owner.setProjectId(saved.getId());
        owner.setUserEmail(ownerEmail);
        owner.setRole(ProjectRole.LEAD);
        memberRepository.save(owner);

        return saved;
    }

    public Project get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException("Project not found", HttpStatus.NOT_FOUND));
    }

    public Project update(UUID id, ProjectUpdateRequest request, String requesterEmail) {
        requireOwner(id, requesterEmail);
        Project project = get(id);
        if (request.name() != null) {
            String trimmed = request.name().trim();
            if (trimmed.isEmpty()) {
                throw new AppException("Project name is required", HttpStatus.BAD_REQUEST);
            }
            project.setName(trimmed);
        }
        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            project.setEndDate(request.endDate());
        }
        return repository.save(project);
    }

    @Transactional
    public void delete(UUID id, String requesterEmail) {
        requireOwner(id, requesterEmail);
        Project project = get(id);
        List<UUID> boardIds = boardRepository.findByProjectId(project.getId()).stream()
                .map(board -> board.getId())
                .toList();

        if (!boardIds.isEmpty()) {
            var taskIds = taskRepository.findByBoardIdIn(boardIds).stream()
                    .map(task -> task.getId())
                    .toList();
            if (!taskIds.isEmpty()) {
                taskCommentRepository.deleteByTaskIdIn(taskIds);
                taskRepository.deleteAllById(taskIds);
            }
            sprintRepository.deleteByBoardIdIn(boardIds);
            boardColumnRepository.deleteByBoardIdIn(boardIds);
            boardMemberRepository.deleteByBoardIdIn(boardIds);
            boardInviteRepository.deleteByBoardIdIn(boardIds);
            boardRepository.deleteAllById(boardIds);
        }

        taskRepository.deleteByProjectId(project.getId());
        inviteRepository.deleteByProjectId(project.getId());
        memberRepository.deleteByProjectId(project.getId());
        repository.deleteById(project.getId());
    }

    public List<Project> listByOwner(String ownerEmail) {
        return repository.findByOwnerEmail(ownerEmail);
    }

    public List<Project> listByMember(String userEmail) {
        return memberRepository.findByUserEmail(userEmail).stream()
                .map(member -> repository.findById(member.getProjectId())
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public boolean isOwner(UUID projectId, String userEmail) {
        return repository.findById(projectId)
                .map(project -> project.getOwnerEmail().equalsIgnoreCase(userEmail))
                .orElse(false);
    }

    private void requireOwner(UUID projectId, String requesterEmail) {
        if (!isOwner(projectId, requesterEmail)) {
            throw new AppException("Only project owner can manage project", HttpStatus.FORBIDDEN);
        }
    }
}
