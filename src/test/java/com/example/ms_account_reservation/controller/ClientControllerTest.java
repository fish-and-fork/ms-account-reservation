package com.example.ms_account_reservation.controller;

import com.example.dto.ClientCreateRequest;
import com.example.dto.ClientExistsResponse;
import com.example.dto.ClientPage;
import com.example.dto.ClientResponse;
import com.example.ms_account_reservation.service.ClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    private final UUID clientId = UUID.randomUUID();
    private final ClientCreateRequest request = mock(ClientCreateRequest.class);
    private final ClientResponse clientResponse = mock(ClientResponse.class);

    @Test
    void createClient_shouldReturn201Created() {
        when(clientService.create(request)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> result = clientController.createClient(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(clientResponse);
        verify(clientService).create(request);
    }

    @Test
    void getClientById_shouldReturn200WithBody() {
        when(clientService.get(clientId)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> result = clientController.getClientById(clientId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(clientResponse);
        verify(clientService).get(clientId);
    }

    @Test
    void checkClientExistence_shouldReturn200() {
        ClientExistsResponse body = mock(ClientExistsResponse.class);
        when(clientService.checkClientExistence(clientId)).thenReturn(body);

        ResponseEntity<ClientExistsResponse> result = clientController.checkClientExistence(clientId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(body);
        verify(clientService).checkClientExistence(clientId);
    }

    @Test
    void updateClient_shouldReturn200() {
        when(clientService.update(clientId, request)).thenReturn(clientResponse);

        ResponseEntity<ClientResponse> result = clientController.updateClient(clientId, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(clientResponse);
        verify(clientService).update(clientId, request);
    }

    @Test
    void deleteClientById_shouldReturn204NoContent() {
        ResponseEntity<Void> result = clientController.deleteClientById(clientId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(clientService).delete(clientId);
    }

    @Test
    void searchClients_shouldPassFiltersToService() {
        ClientPage page = mock(ClientPage.class);
        when(clientService.searchClients(1, 50, "Ivan", 123L)).thenReturn(page);

        ResponseEntity<ClientPage> result = clientController.searchClients(1, 50, "Ivan", 123L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(page);
        verify(clientService).searchClients(1, 50, "Ivan", 123L);
    }

    @Test
    void searchClients_shouldApplyDefaultsWhenNull() {
        ClientPage page = mock(ClientPage.class);
        when(clientService.searchClients(0, 20, null, null)).thenReturn(page);

        ResponseEntity<ClientPage> result = clientController.searchClients(null, null, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(page);
        // The controller substitutes defaults 0 / 20 for null page/size
        verify(clientService).searchClients(0, 20, null, null);
    }
}