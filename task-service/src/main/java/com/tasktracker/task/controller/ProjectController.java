package com.tasktracker.task.controller;

import com.tasktracker.task.dto.ProjectMemberRequest;
import com.tasktracker.task.dto.ProjectMemberResponse;
import com.tasktracker.task.dto.ProjectRequest;
import com.tasktracker.task.dto.ProjectResponse;
import com.tasktracker.task.dto.ProjectUpdateRequest;
import com.tasktracker.task.dto.ProjectInviteRequest;
import com.tasktracker.task.dto.ProjectInviteResponse;
import com.tasktracker.task.entity.ProjectRole;
import com.tasktracker.task.service.ProjectMemberService;
import com.tasktracker.task.service.ProjectService;
import com.tasktracker.task.service.ProjectInviteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService service;
    private final ProjectMemberService memberService;
    private final ProjectInviteService inviteService;

    public ProjectController(ProjectService service,
                             ProjectMemberService memberService,
                             ProjectInviteService inviteService) {
        this.service = service;
        this.memberService = memberService;
        this.inviteService = inviteService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody ProjectRequest request) {
        var project = service.create(request, jwt.getSubject());
        return ResponseEntity.ok(toResponse(project));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> myProjects(@AuthenticationPrincipal Jwt jwt) {
        var projects = service.listByMember(jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(toResponse(service.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(@PathVariable("id") UUID id,
                                                  @AuthenticationPrincipal Jwt jwt,
                                                  @RequestBody ProjectUpdateRequest request) {
        var project = service.update(id, request, jwt.getSubject());
        return ResponseEntity.ok(toResponse(project));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id,
                                       @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ProjectMemberResponse> addMember(@PathVariable("id") UUID id,
                                                           @AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody ProjectMemberRequest request) {
        var member = memberService.addMember(id, request, jwt.getSubject());
        return ResponseEntity.ok(toResponse(member));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectMemberResponse>> listMembers(@PathVariable("id") UUID id,
                                                                    @AuthenticationPrincipal Jwt jwt) {
        var members = memberService.listMembers(id, jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(members);
    }

    @PutMapping("/members/{memberId}/role")
    public ResponseEntity<ProjectMemberResponse> updateRole(@PathVariable("memberId") UUID memberId,
                                                            @AuthenticationPrincipal Jwt jwt,
                                                            @RequestBody ProjectRole role) {
        var member = memberService.updateRole(memberId, role, jwt.getSubject());
        return ResponseEntity.ok(toResponse(member));
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable("memberId") UUID memberId,
                                             @AuthenticationPrincipal Jwt jwt) {
        memberService.removeMember(memberId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/me")
    public ResponseEntity<Void> leaveProject(@PathVariable("id") UUID id,
                                             @AuthenticationPrincipal Jwt jwt) {
        memberService.leaveProject(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/invites")
    public ResponseEntity<ProjectInviteResponse> createInvite(@PathVariable("id") UUID id,
                                                              @AuthenticationPrincipal Jwt jwt,
                                                              @Valid @RequestBody ProjectInviteRequest request) {
        var invite = inviteService.createInvite(id, request, jwt.getSubject());
        return ResponseEntity.ok(toResponse(invite));
    }

    @PostMapping("/invites/{token}/accept")
    public ResponseEntity<ProjectInviteResponse> acceptInvite(@PathVariable("token") String token,
                                                              @AuthenticationPrincipal Jwt jwt) {
        var invite = inviteService.acceptInvite(token, jwt.getSubject());
        return ResponseEntity.ok(toResponse(invite));
    }

    private ProjectResponse toResponse(com.tasktracker.task.entity.Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getOwnerEmail(),
                project.getStartDate(),
                project.getEndDate()
        );
    }

    private ProjectMemberResponse toResponse(com.tasktracker.task.entity.ProjectMember member) {
        return new ProjectMemberResponse(member.getId(), member.getProjectId(), member.getUserEmail(), member.getRole());
    }

    private ProjectInviteResponse toResponse(com.tasktracker.task.entity.ProjectInvite invite) {
        return new ProjectInviteResponse(
                invite.getId(),
                invite.getProjectId(),
                invite.getUserEmail(),
                invite.getRole(),
                invite.getToken(),
                invite.getCreatedBy(),
                invite.getExpiresAt(),
                invite.isActive()
        );
    }
}
