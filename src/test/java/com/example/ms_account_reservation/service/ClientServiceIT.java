package com.example.ms_account_reservation.service;

import com.example.dto.ClientCreateRequest;
import com.example.dto.ClientResponse;
import com.example.ms_account_reservation.entity.Client;
import com.example.ms_account_reservation.model.ClientType;
import com.example.ms_account_reservation.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Testcontainers
class ClientServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    ClientService clientService;
    @Autowired
    ClientRepository clientRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE client CASCADE");
    }

    @Test
    void update_persistsChangesToDatabase() {
        Client saved = clientRepository.saveAndFlush(buildClient(100L, "Аль", "Пачино"));

        ClientResponse result = clientService.update(saved.getId(),
                requestWith(100L, "Сильвестр", "Сталлоне"));

        assertThat(result.getFirstName()).isEqualTo("Сильвестр");

        Client reloaded = clientRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFirstName()).isEqualTo("Сильвестр");
    }

    @Test
    void create_setsTimestampsBeforeMappingToResponse() {
        ClientResponse response = clientService.create(requestWith(100L, "Аль", "Пачино"));

        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void delete_persistsSoftDeleteToDatabase() {
        Client saved = clientRepository.saveAndFlush(buildClient(100L, "Аль", "Пачино"));

        clientService.delete(saved.getId());

        Client reloaded = clientRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getClientType()).isEqualTo(ClientType.DELETED);
    }

    private static ClientCreateRequest requestWith(long mdmCode, String firstName, String lastName) {
        ClientCreateRequest r = new ClientCreateRequest();
        r.setMdmCode(mdmCode);
        r.setFirstName(firstName);
        r.setLastName(lastName);
        return r;
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
                .orElseThrow(() -> new IllegalStateException("No non-DELETED ClientType"));
    }
}