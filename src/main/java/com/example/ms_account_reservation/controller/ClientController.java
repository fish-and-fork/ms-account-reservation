package com.example.ms_account_reservation.controller;

import com.example.api.ClientsApi;
import com.example.dto.ClientCreateRequest;
import com.example.dto.ClientExistsResponse;
import com.example.dto.ClientPage;
import com.example.dto.ClientResponse;
import com.example.ms_account_reservation.service.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Slf4j
public class ClientController implements ClientsApi {

    private final ClientService clientService;

    @Autowired
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    public ResponseEntity<ClientResponse> createClient(ClientCreateRequest clientCreateRequest) {
        ClientResponse response = clientService.create(clientCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ClientResponse> getClientById(UUID clientId) {
        ClientResponse response = clientService.get(clientId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    public ResponseEntity<ClientPage> searchClients(Integer page, Integer size,
                                                    @Nullable String lastName, @Nullable Long mdmCode) {
        ClientPage result = clientService.searchClients(
                (page != null) ? page : 0,
                (size != null) ? size : 20,
                lastName, mdmCode
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @Override
    public ResponseEntity<ClientResponse> updateClient(UUID clientId, ClientCreateRequest clientCreateRequest) {
        ClientResponse response = clientService.update(clientId, clientCreateRequest);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Override
    public ResponseEntity<Void> deleteClientById(UUID clientId) {
        clientService.delete(clientId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ClientExistsResponse> checkClientExistence(UUID clientId) {
        ClientExistsResponse response = clientService.checkClientExistence(clientId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
