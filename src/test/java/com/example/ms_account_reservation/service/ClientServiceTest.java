package com.example.ms_account_reservation.service;

import com.example.dto.ClientCreateRequest;
import com.example.dto.ClientExistsResponse;
import com.example.dto.ClientPage;
import com.example.dto.ClientResponse;
import com.example.ms_account_reservation.entity.Client;
import com.example.ms_account_reservation.exception.ApiException;
import com.example.ms_account_reservation.mapper.ClientMapper;
import com.example.ms_account_reservation.model.ClientType;
import com.example.ms_account_reservation.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ClientServiceTest {

    private ClientRepository clientRepository;
    private ClientMapper clientMapper;
    private ClientService clientService;

    private final UUID clientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        clientRepository = mock(ClientRepository.class);
        clientMapper = mock(ClientMapper.class);
        clientService = new ClientService(clientRepository, clientMapper);
    }

    // === create ===

    @Test
    void create_shouldSaveAndReturnResponse() {
        ClientCreateRequest request = mock(ClientCreateRequest.class);
        when(request.getMdmCode()).thenReturn(123L);

        Client client = mock(Client.class);
        ClientResponse response = mock(ClientResponse.class);

        when(clientRepository.existsByMdmCode(123L)).thenReturn(false);
        when(clientMapper.toEntity(request)).thenReturn(client);
        when(clientRepository.saveAndFlush(client)).thenReturn(client);
        when(clientMapper.toResponse(client)).thenReturn(response);

        ClientResponse result = clientService.create(request);

        assertThat(result).isSameAs(response);
        verify(clientRepository).existsByMdmCode(123L);
        verify(clientMapper).toEntity(request);
        verify(clientRepository).saveAndFlush(client);
        verify(clientMapper).toResponse(client);
        verify(response).setHasAccounts(false);
    }

    @Test
    void create_shouldThrowApiException_whenMdmCodeAlreadyExists() {
        ClientCreateRequest request = mock(ClientCreateRequest.class);
        when(request.getMdmCode()).thenReturn(123L);
        when(clientRepository.existsByMdmCode(123L)).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("mdmCode=123");

        verify(clientRepository, never()).saveAndFlush(any());
        verifyNoInteractions(clientMapper);
    }

    // === get ===

    @Test
    void get_shouldReturnResponse_whenClientFound() {
        Client client = mock(Client.class);
        ClientResponse response = mock(ClientResponse.class);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientMapper.toResponse(client)).thenReturn(response);

        ClientResponse result = clientService.get(clientId);

        assertThat(result).isSameAs(response);
        verify(response).setHasAccounts(false);
    }

    @Test
    void get_shouldThrowApiException_whenClientNotFound() {
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.get(clientId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("id=" + clientId);

        verifyNoInteractions(clientMapper);
    }

    // === searchClients ===

    @Test
    @SuppressWarnings("unchecked")
    void searchClients_shouldMapContentAndFillPageMetadata() {
        Client c1 = mock(Client.class);
        Client c2 = mock(Client.class);
        ClientResponse r1 = mock(ClientResponse.class);
        ClientResponse r2 = mock(ClientResponse.class);
        when(clientMapper.toResponse(c1)).thenReturn(r1);
        when(clientMapper.toResponse(c2)).thenReturn(r2);

        Page<Client> page = mock(Page.class);
        when(page.getContent()).thenReturn(List.of(c1, c2));
        when(page.getNumber()).thenReturn(0);
        when(page.getSize()).thenReturn(20);
        when(page.getTotalElements()).thenReturn(2L);
        when(page.getTotalPages()).thenReturn(1);
        when(page.isFirst()).thenReturn(true);
        when(page.isLast()).thenReturn(true);

        when(clientRepository.findAll(
                ArgumentMatchers.<Specification<Client>>any(),
                any(Pageable.class)
        )).thenReturn(page);

        ClientPage result = clientService.searchClients(0, 20, "Ivan", 123L);

        assertThat(result.getContent()).containsExactly(r1, r2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getTotalPages()).isEqualTo(1);
        // если DTO сгенерирован с примитивом boolean — замените на isFirst()/isLast()
        assertThat(result.getFirst()).isTrue();
        assertThat(result.getLast()).isTrue();

        verify(r1).setHasAccounts(false);
        verify(r2).setHasAccounts(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchClients_shouldPassExpectedPaginationAndSorting() {
        Page<Client> empty = mock(Page.class);
        when(empty.getContent()).thenReturn(List.of());
        when(clientRepository.findAll(
                ArgumentMatchers.<Specification<Client>>any(),
                any(Pageable.class)
        )).thenReturn(empty);

        clientService.searchClients(2, 50, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(clientRepository).findAll(
                ArgumentMatchers.<Specification<Client>>any(),
                captor.capture()
        );

        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(50);

        Sort sort = pageable.getSort();
        Sort.Order lastName = sort.getOrderFor("lastName");
        assertThat(lastName).isNotNull();
        assertThat(lastName.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(sort.getOrderFor("firstName")).isNotNull();
        assertThat(sort.getOrderFor("mdmCode")).isNotNull();
    }

    // === checkClientExistence ===

    @Test
    void checkClientExistence_shouldReturnExistsTrue_whenClientFound() {
        Client client = mock(Client.class);
        ClientType type = firstClientTypeWithMatchingDtoEnum();
        when(client.getId()).thenReturn(clientId);
        when(client.getClientType()).thenReturn(type);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        ClientExistsResponse result = clientService.checkClientExistence(clientId);

        // если DTO сгенерирован с примитивом boolean — замените на isExists()
        assertThat(result.getExists()).isTrue();
        assertThat(result.getId()).isEqualTo(clientId);
        assertThat(result.getClientType())
                .isEqualTo(ClientExistsResponse.ClientTypeEnum.valueOf(type.name()));
    }

    @Test
    void checkClientExistence_shouldReturnExistsFalse_whenClientNotFound() {
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        ClientExistsResponse result = clientService.checkClientExistence(clientId);

        assertThat(result.getExists()).isFalse();
        assertThat(result.getId()).isNull();
        assertThat(result.getClientType()).isNull();
    }

    // === update ===

    @Test
    void update_shouldMapAndReturnResponse_whenClientFound() {
        ClientCreateRequest request = mock(ClientCreateRequest.class);
        Client client = mock(Client.class);
        ClientResponse response = mock(ClientResponse.class);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(clientMapper.toResponse(client)).thenReturn(response);

        ClientResponse result = clientService.update(clientId, request);

        assertThat(result).isSameAs(response);
        verify(clientMapper).updateEntity(request, client);
        verify(clientRepository).flush();
        verify(response).setHasAccounts(false);
    }

    @Test
    void update_shouldThrowApiException_whenClientNotFound() {
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.update(clientId, mock(ClientCreateRequest.class)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("id=" + clientId);

        verify(clientMapper, never()).updateEntity(any(), any());
        verify(clientRepository, never()).flush();
    }

    // === delete ===

    @Test
    void delete_shouldMarkClientAsDeleted() {
        Client client = mock(Client.class);
        when(client.getClientType()).thenReturn(anyNonDeletedType());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        clientService.delete(clientId);

        verify(client).setClientType(ClientType.DELETED);
        // репозиторий не сохраняет — расчёт на dirty checking внутри @Transactional
        verify(clientRepository, never()).saveAndFlush(any());
        verify(clientRepository, never()).flush();
    }

    @Test
    void delete_shouldBeIdempotent_whenClientAlreadyDeleted() {
        Client client = mock(Client.class);
        when(client.getClientType()).thenReturn(ClientType.DELETED);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        clientService.delete(clientId);

        verify(client, never()).setClientType(any());
    }

    @Test
    void delete_shouldThrowApiException_whenClientNotFound() {
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.delete(clientId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("id=" + clientId);
    }

    // === helpers ===

    private static ClientType anyNonDeletedType() {
        return Arrays.stream(ClientType.values())
                .filter(t -> t != ClientType.DELETED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No non-DELETED ClientType"));
    }

    /**
     * Подбирает ClientType, для которого существует одноимённая константа
     * в ClientExistsResponse.ClientTypeEnum — чтобы checkClientExistence не упал
     * на valueOf(...) и тест не зависел от конкретного набора констант
     */
    private static ClientType firstClientTypeWithMatchingDtoEnum() {
        for (ClientType t : ClientType.values()) {
            try {
                ClientExistsResponse.ClientTypeEnum.valueOf(t.name());
                return t;
            } catch (IllegalArgumentException ignored) {
                // нет пары — пробуем следующую
            }
        }
        throw new IllegalStateException("No ClientType has matching ClientTypeEnum");
    }
}