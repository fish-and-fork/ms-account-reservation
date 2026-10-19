package com.example.ms_account_reservation.service;

import com.example.dto.ClientCreateRequest;
import com.example.dto.ClientResponse;
import com.example.ms_account_reservation.entity.Client;
import com.example.ms_account_reservation.exception.ApiError;
import com.example.ms_account_reservation.exception.ApiException;
import com.example.ms_account_reservation.mapper.ClientMapper;
import com.example.ms_account_reservation.model.ClientType;
import com.example.ms_account_reservation.repository.ClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Autowired
    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
    }

    @Transactional
    public ClientResponse create(ClientCreateRequest clientCreateRequest) {
        checkIfMdmUnique(clientCreateRequest.getMdmCode());
        Client client = clientMapper.toEntity(clientCreateRequest);
        Client savedClient = clientRepository.saveAndFlush(client); // flush чтобы обновить @UpdateTimestamp
        log.info("Клиент создан: id={}, mdmCode={}", savedClient.getId(), savedClient.getMdmCode());
        ClientResponse response = clientMapper.toResponse(savedClient);
        response.setHasAccounts(false);
        return response;
    }

    public ClientResponse get(UUID clientId) {
        Client client = getClientOrThrow(clientId);
        ClientResponse response = clientMapper.toResponse(client);
        response.setHasAccounts(false);
        return response;
    }

    @Transactional
    public ClientResponse update(UUID clientId, ClientCreateRequest clientCreateRequest) {
        Client client = getClientOrThrow(clientId);
        clientMapper.updateEntity(clientCreateRequest, client);
        clientRepository.flush(); // чтобы обновить @UpdateTimestamp
        log.info("Клиент обновлён: id={}, mdmCode={}", client.getId(), client.getMdmCode());
        ClientResponse response = clientMapper.toResponse(client);
        response.setHasAccounts(false);
        return response;
    }

    @Transactional
    public void delete(UUID clientId) {
        Client client = getClientOrThrow(clientId);
        if (client.getClientType() == ClientType.DELETED) {
            return;
        }
        boolean hasAccounts = false; // todo
        if (hasAccounts) {
            throw new ApiException(
                    ApiError.CLIENT_HAS_ACTIVE_ACCOUNTS,
                    "Нельзя удалить клиента с mdmCode=" + client.getMdmCode() + ", т.к. у него есть счет(а)"
            );
        }
        client.setClientType(ClientType.DELETED);
        log.info("Клиент удалён (мягкое удаление): id={}, mdmCode={}", client.getId(), client.getMdmCode());
    }

    private void checkIfMdmUnique(Long mdmCode) {
        if (clientRepository.existsByMdmCode(mdmCode)) {
            throw new ApiException(
                    ApiError.CLIENT_MDM_EXISTS,
                    "Клиент с mdmCode=" + mdmCode + " уже существует");
        }
    }

    private Client getClientOrThrow(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ApiException(
                        ApiError.CLIENT_NOT_FOUND,
                        "Клиент с id=" + clientId + " не найден"
                ));
    }
}
