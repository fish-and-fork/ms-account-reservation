package com.example.ms_account_reservation.service;

import com.example.dto.ClientCreateRequest;
import com.example.dto.ClientResponse;
import com.example.ms_account_reservation.entity.Client;
import com.example.ms_account_reservation.exception.ApiError;
import com.example.ms_account_reservation.exception.ApiException;
import com.example.ms_account_reservation.mapper.ClientMapper;
import com.example.ms_account_reservation.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Autowired
    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
    }

    public ClientResponse create(ClientCreateRequest clientCreateRequest) {
        Client client = clientMapper.toEntity(clientCreateRequest);
        Client savedClient = clientRepository.save(client);
        return clientMapper.toResponse(savedClient);
    }

    public ClientResponse get(UUID clientId) {
        Client client = this.clientRepository.findById(clientId)
                .orElseThrow(() -> new ApiException(
                        ApiError.CLIENT_NOT_FOUND,
                        "Клиент с id=" + clientId + " не найден" // лог
                ));
        ClientResponse response = clientMapper.toResponse(client);
        response.setHasAccounts(false);
        return response;
    }
}
