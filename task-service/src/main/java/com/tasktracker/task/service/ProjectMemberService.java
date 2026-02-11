package com.tasktracker.task.service;

import com.tasktracker.task.dto.ProjectMemberRequest;
import com.tasktracker.task.entity.ProjectMember;
import com.tasktracker.task.entity.ProjectRole;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.repository.ProjectMemberRepository;
import com.tasktracker.task.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectMemberService {

    private final ProjectMemberRepository repository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;

    public ProjectMemberService(ProjectMemberRepository repository,
                                ProjectRepository projectRepository,
                                ProjectService projectService) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
    }

    public ProjectMember addMember(UUID projectId, ProjectMemberRequest request, String requesterEmail) {
        requireOwner(projectId, requesterEmail);
        if (!projectRepository.existsById(projectId)) {
            throw new AppException("Project not found", HttpStatus.NOT_FOUND);
        }
        return repository.findByProjectIdAndUserEmail(projectId, request.email())
                .orElseGet(() -> {
                    ProjectMember member = new ProjectMember();
                    member.setProjectId(projectId);
                    member.setUserEmail(request.email());
                    member.setRole(request.role() != null ? request.role() : ProjectRole.VIEWER);
                    return repository.save(member);
                });
    }

    public List<ProjectMember> listMembers(UUID projectId, String requesterEmail) {
        if (!projectService.isOwner(projectId, requesterEmail) &&
                repository.findByProjectIdAndUserEmail(projectId, requesterEmail).isEmpty()) {
            throw new AppException("Project access denied", HttpStatus.FORBIDDEN);
        }
        return repository.findByProjectId(projectId);
    }

    public ProjectMember updateRole(UUID memberId, ProjectRole role, String requesterEmail) {
        ProjectMember member = repository.findById(memberId)
                .orElseThrow(() -> new AppException("Member not found", HttpStatus.NOT_FOUND));
        requireOwner(member.getProjectId(), requesterEmail);
        member.setRole(role);
        return repository.save(member);
    }

    public void removeMember(UUID memberId, String requesterEmail) {
        ProjectMember member = repository.findById(memberId)
                .orElseThrow(() -> new AppException("Member not found", HttpStatus.NOT_FOUND));
        requireOwner(member.getProjectId(), requesterEmail);
        repository.deleteById(memberId);
    }

    public void leaveProject(UUID projectId, String requesterEmail) {
        if (projectService.isOwner(projectId, requesterEmail)) {
            throw new AppException("Owner cannot leave project", HttpStatus.CONFLICT);
        }
        ProjectMember member = repository.findByProjectIdAndUserEmail(projectId, requesterEmail)
                .orElseThrow(() -> new AppException("Membership not found", HttpStatus.NOT_FOUND));
        repository.deleteById(member.getId());
    }

    private void requireOwner(UUID projectId, String requesterEmail) {
        if (!projectService.isOwner(projectId, requesterEmail)) {
            throw new AppException("Only project owner can manage members", HttpStatus.FORBIDDEN);
        }
    }
}
