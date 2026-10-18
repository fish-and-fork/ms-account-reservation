package com.example.ms_account_reservation.controller;

import com.example.api.ClientsApi;
import com.example.dto.ClientCreateRequest;
import com.example.dto.ClientResponse;
import com.example.ms_account_reservation.service.ClientService;
import lombok.extern.slf4j.Slf4j;
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
}
