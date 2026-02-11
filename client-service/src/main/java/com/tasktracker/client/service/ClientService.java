package com.tasktracker.client.service;

import com.tasktracker.client.dto.ClientCreateRequest;
import com.tasktracker.client.dto.ClientUpdateRequest;
import com.tasktracker.client.entity.Client;
import com.tasktracker.client.exception.AppException;
import com.tasktracker.client.repository.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    public Client create(ClientCreateRequest request) {
        Client client = new Client();
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setCompany(request.company());
        client.setNotes(request.notes());
        return repository.save(client);
    }

    public Client update(UUID id, ClientUpdateRequest request) {
        Client client = repository.findById(id)
                .orElseThrow(() -> new AppException("Client not found", HttpStatus.NOT_FOUND));
        if (request.name() != null) client.setName(request.name());
        if (request.email() != null) client.setEmail(request.email());
        if (request.phone() != null) client.setPhone(request.phone());
        if (request.company() != null) client.setCompany(request.company());
        if (request.notes() != null) client.setNotes(request.notes());
        return repository.save(client);
    }

    public Client get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException("Client not found", HttpStatus.NOT_FOUND));
    }

    public List<Client> list() {
        return repository.findAll();
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new AppException("Client not found", HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }
}
