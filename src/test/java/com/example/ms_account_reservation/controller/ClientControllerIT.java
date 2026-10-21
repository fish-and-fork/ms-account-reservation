package com.example.ms_account_reservation.controller;

import com.example.ms_account_reservation.entity.Client;
import com.example.ms_account_reservation.model.ClientType;
import com.example.ms_account_reservation.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Testcontainers
class ClientControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ClientRepository clientRepository;

    // === POST /clients ===

    @Test
    void createClient_returns201AndBody() throws Exception {
        String body = createClientJson(100L, "Аль", "Пачино",
                "1111", "111-111", " Паспорт");

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.mdmCode").value(100))
                .andExpect(jsonPath("$.firstName").value("Аль"))
                .andExpect(jsonPath("$.lastName").value("Пачино"))
                .andExpect(jsonPath("$.hasAccounts").value(false)); // todo

        assertThat(clientRepository.existsByMdmCode(100L)).isTrue();
    }

    @Test
    void createClient_withDuplicateMdmCode_returns409() throws Exception {
        clientRepository.saveAndFlush(buildClient(100L, "Аль", "Пачино"));

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createClientJson(100L, "Сильвестр", "Сталлоне",
                                "1111", "111-111", " Паспорт")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").exists());
    }

    @Test
    void createClient_withInvalidBody_returns400() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // === GET /clients/{clientId} ===

    @Test
    void getClientById_returns200() throws Exception {
        Client saved = clientRepository.saveAndFlush(buildClient(100L, "Аль", "Пачино"));

        mockMvc.perform(get("/clients/{id}", saved.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.mdmCode").value(100))
                .andExpect(jsonPath("$.hasAccounts").value(false)); // todo
    }

    @Test
    void getClientById_whenMissing_returns404() throws Exception {
        mockMvc.perform(get("/clients/{id}", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // === GET /clients/{clientId}/exists ===

    @Test
    void checkClientExistence_whenExists_returns200WithDetails() throws Exception {
        ClientType type = anyNonDeletedType();
        Client saved = clientRepository.saveAndFlush(buildClient(100L, type, "Аль", "Пачино"));

        mockMvc.perform(get("/clients/{id}/exists", saved.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.clientType").value(type.name()));
    }

    @Test
    void checkClientExistence_whenMissing_returns200WithExistsFalse() throws Exception {
        mockMvc.perform(get("/clients/{id}/exists", UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.clientType").doesNotExist());
    }

    // === GET /clients ===

    @Test
    void searchClients_returnsPageWithFilters() throws Exception {
        clientRepository.saveAndFlush(buildClient(100L, "Аль", "Пачино"));
        clientRepository.saveAndFlush(buildClient(200L, "Сильвестр", "Сталлоне"));

        mockMvc.perform(get("/clients")
                        .param("lastName", "Пачино")   // проверяем case-insensitive
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].mdmCode").value(100))
                .andExpect(jsonPath("$.content[0].hasAccounts").value(false)); // todo
    }

    @Test
    void searchClients_withTooLargeSize_returns400() throws Exception {
        mockMvc.perform(get("/clients")
                        .param("size", "101")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchClients_excludesSoftDeletedClients() throws Exception {
        Client alive = clientRepository.saveAndFlush(buildClient(100L, "Аль", "Пачино"));
        Client deleted = clientRepository.saveAndFlush(buildClient(200L, "Сильвестр", "Сталлоне"));
        deleted.setClientType(ClientType.DELETED);
        clientRepository.flush();

        mockMvc.perform(get("/clients").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(alive.getId().toString()));
    }

    // === PUT /clients/{clientId} ===

    @Test
    void updateClient_returns200() throws Exception {
        Client saved = clientRepository.saveAndFlush(buildClient(100L, "Аль", "Пачино"));

        mockMvc.perform(put("/clients/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createClientJson(100L, "Сильвестр", "Сталлоне",
                                "1111", "111-111", " Паспорт")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Сильвестр"))
                .andExpect(jsonPath("$.lastName").value("Сталлоне"));
    }

    @Test
    void updateClient_whenMissing_returns404() throws Exception {
        mockMvc.perform(put("/clients/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createClientJson(100L, "Аль", "Пачино",
                                "1111", "111-111", " Паспорт")))
                .andExpect(status().isNotFound());
    }

    // === DELETE /clients/{clientId} ===

    @Test
    void deleteClient_returns204_andExcludesFromSearch() throws Exception {
        Client saved = clientRepository.saveAndFlush(buildClient(100L, "Аль", "Пачино"));

        mockMvc.perform(delete("/clients/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/clients").accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deleteClient_whenMissing_returns404() throws Exception {
        mockMvc.perform(delete("/clients/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteClient_isIdempotent() throws Exception {
        Client saved = clientRepository.saveAndFlush(buildClient(100L, "Аль", "Пачино"));

        mockMvc.perform(delete("/clients/{id}", saved.getId())).andExpect(status().isNoContent());
        mockMvc.perform(delete("/clients/{id}", saved.getId())).andExpect(status().isNoContent());
    }

    // ======

    private static String createClientJson(long mdmCode, String firstName, String lastName,
        String documentSeries, String documentNumber, String documentType ) {

        return """
                {
                  "mdmCode": %d,
                  "firstName": "%s",
                  "lastName": "%s",
                  "clientType": "%s",
                  "documentSeries": "%s",
                  "documentNumber": "%s",
                  "documentType": "%s"
                }
                """.formatted(mdmCode, firstName, lastName, anyNonDeletedType().name(),
                    documentSeries, documentNumber, documentType
                );
    }

    private static Client buildClient(long mdmCode, String firstName, String lastName) {
        return buildClient(mdmCode, anyNonDeletedType(), firstName, lastName);
    }

    private static Client buildClient(long mdmCode, ClientType type,
                                      String firstName, String lastName) {
        Client c = new Client();
        c.setMdmCode(mdmCode);
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setClientType(type);
        return c;
    }

    private static ClientType anyNonDeletedType() {
        return Arrays.stream(ClientType.values())
                .filter(t -> t != ClientType.DELETED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет не-DELETED ClientType"));
    }
}