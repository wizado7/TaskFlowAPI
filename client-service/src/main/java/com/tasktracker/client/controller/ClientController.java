package com.tasktracker.client.controller;

import com.tasktracker.client.dto.ClientCreateRequest;
import com.tasktracker.client.dto.ClientResponse;
import com.tasktracker.client.dto.ClientUpdateRequest;
import com.tasktracker.client.entity.Client;
import com.tasktracker.client.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientCreateRequest request) {
        return ResponseEntity.ok(toResponse(service.create(request)));
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> list() {
        return ResponseEntity.ok(service.list().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(service.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> update(@PathVariable UUID id, @RequestBody ClientUpdateRequest request) {
        return ResponseEntity.ok(toResponse(service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getCompany(),
                client.getNotes()
        );
    }
}
