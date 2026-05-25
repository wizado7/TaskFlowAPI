package com.tasktracker.client.controller;

import com.tasktracker.client.dto.ClientCreateRequest;
import com.tasktracker.client.dto.ClientAttachmentResponse;
import com.tasktracker.client.dto.ClientCommentRequest;
import com.tasktracker.client.dto.ClientCommentResponse;
import com.tasktracker.client.dto.ClientResponse;
import com.tasktracker.client.dto.ClientUpdateRequest;
import com.tasktracker.client.entity.Client;
import com.tasktracker.client.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody ClientCreateRequest request) {
        return ResponseEntity.ok(toResponse(service.create(request, jwt.getSubject(), jwt.getTokenValue())));
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> list(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam("projectId") UUID projectId) {
        return ResponseEntity.ok(service.list(projectId, jwt.getTokenValue()).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> get(@PathVariable("id") UUID id,
                                              @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(toResponse(service.get(id, jwt.getTokenValue())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> update(@PathVariable("id") UUID id,
                                                 @AuthenticationPrincipal Jwt jwt,
                                                 @RequestBody ClientUpdateRequest request) {
        return ResponseEntity.ok(toResponse(service.update(id, request, jwt.getTokenValue())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id,
                                       @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, jwt.getTokenValue());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ClientCommentResponse> addComment(@PathVariable("id") UUID id,
                                                            @AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody ClientCommentRequest request) {
        return ResponseEntity.ok(toResponse(service.addComment(id, jwt.getSubject(), request.message(), jwt.getTokenValue())));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<ClientCommentResponse>> listComments(@PathVariable("id") UUID id,
                                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.listComments(id, jwt.getTokenValue()).stream().map(this::toResponse).toList());
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClientAttachmentResponse> uploadAttachment(@PathVariable("id") UUID id,
                                                                     @AuthenticationPrincipal Jwt jwt,
                                                                     @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(toResponse(service.uploadAttachment(id, jwt.getSubject(), file, jwt.getTokenValue())));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<ClientAttachmentResponse>> listAttachments(@PathVariable("id") UUID id,
                                                                         @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.listAttachments(id, jwt.getTokenValue()).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") UUID id,
                                                       @PathVariable("attachmentId") UUID attachmentId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        var download = service.downloadAttachment(id, attachmentId, jwt.getTokenValue());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.contentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.originalFileName()).build().toString())
                .body(download.resource());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable("id") UUID id,
                                                 @PathVariable("attachmentId") UUID attachmentId,
                                                 @AuthenticationPrincipal Jwt jwt) {
        service.deleteAttachment(id, attachmentId, jwt.getSubject(), jwt.getTokenValue());
        return ResponseEntity.noContent().build();
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getProjectId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getCompany(),
                client.getNotes(),
                client.getStage().name(),
                client.getCreatedBy()
        );
    }

    private ClientCommentResponse toResponse(com.tasktracker.client.entity.ClientComment comment) {
        return new ClientCommentResponse(
                comment.getId(),
                comment.getClientId(),
                comment.getAuthorEmail(),
                comment.getMessage(),
                comment.getCreatedAt()
        );
    }

    private ClientAttachmentResponse toResponse(com.tasktracker.client.entity.ClientAttachment attachment) {
        return new ClientAttachmentResponse(
                attachment.getId(),
                attachment.getClientId(),
                attachment.getUploaderEmail(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getCreatedAt()
        );
    }
}
