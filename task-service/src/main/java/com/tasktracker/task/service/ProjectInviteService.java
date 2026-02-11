package com.tasktracker.task.service;

import com.tasktracker.task.dto.ProjectInviteRequest;
import com.tasktracker.task.entity.ProjectInvite;
import com.tasktracker.task.entity.ProjectMember;
import com.tasktracker.task.entity.ProjectRole;
import com.tasktracker.task.exception.AppException;
import com.tasktracker.task.repository.ProjectInviteRepository;
import com.tasktracker.task.repository.ProjectMemberRepository;
import com.tasktracker.task.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProjectInviteService {

    private final ProjectInviteRepository repository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectService projectService;
    private final NotificationClient notificationClient;

    public ProjectInviteService(ProjectInviteRepository repository,
                                ProjectRepository projectRepository,
                                ProjectMemberRepository memberRepository,
                                ProjectService projectService,
                                NotificationClient notificationClient) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.projectService = projectService;
        this.notificationClient = notificationClient;
    }

    public ProjectInvite createInvite(UUID projectId, ProjectInviteRequest request, String inviterEmail) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException("Project not found", HttpStatus.NOT_FOUND));
        if (!projectService.isOwner(projectId, inviterEmail)) {
            throw new AppException("Only project owner can invite", HttpStatus.FORBIDDEN);
        }
        if (memberRepository.findByProjectIdAndUserEmail(projectId, request.email()).isPresent()) {
            throw new AppException("User already in project", HttpStatus.CONFLICT);
        }
        ProjectInvite invite = repository.findByProjectIdAndUserEmail(projectId, request.email())
                .orElseGet(ProjectInvite::new);
        invite.setProjectId(projectId);
        invite.setUserEmail(request.email());
        invite.setRole(request.role() != null ? request.role() : ProjectRole.EMPLOYEE);
        invite.setToken(UUID.randomUUID().toString());
        invite.setCreatedBy(inviterEmail);
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        invite.setActive(true);
        ProjectInvite saved = repository.save(invite);

        notificationClient.sendProjectInvite(request.email(), project.getName(), saved.getToken(), inviterEmail);
        return saved;
    }

    public ProjectInvite acceptInvite(String token, String userEmail) {
        ProjectInvite invite = repository.findByToken(token)
                .orElseThrow(() -> new AppException("Invite not found", HttpStatus.NOT_FOUND));
        if (!invite.isActive()) {
            throw new AppException("Invite is inactive", HttpStatus.CONFLICT);
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException("Invite expired", HttpStatus.CONFLICT);
        }
        if (!invite.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new AppException("Invite is for another user", HttpStatus.FORBIDDEN);
        }

        memberRepository.findByProjectIdAndUserEmail(invite.getProjectId(), userEmail)
                .orElseGet(() -> {
                    ProjectMember member = new ProjectMember();
                    member.setProjectId(invite.getProjectId());
                    member.setUserEmail(userEmail);
                    member.setRole(invite.getRole());
                    return memberRepository.save(member);
                });

        invite.setActive(false);
        return repository.save(invite);
    }
}
