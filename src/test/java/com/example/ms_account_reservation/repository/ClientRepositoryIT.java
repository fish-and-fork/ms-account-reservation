package com.example.ms_account_reservation.repository;

import com.example.ms_account_reservation.entity.Client;
import com.example.ms_account_reservation.model.ClientType;
import com.example.ms_account_reservation.specification.ClientSpecifications;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // отключает подмену на H2 (подменяется из-за @DataJpaTest)
@Testcontainers
class ClientRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    ClientRepository clientRepository;

    @Test
    void existsByMdmCode_returnsTrueOnlyForExistingCode() {
        clientRepository.saveAndFlush(buildClient(100L, anyNonDeletedType()));

        assertThat(clientRepository.existsByMdmCode(100L)).isTrue();
        assertThat(clientRepository.existsByMdmCode(999L)).isFalse();
    }

    @Test
    void save_generatesIdAndTimestamps() {
        Client saved = clientRepository.saveAndFlush(buildClient(100L, anyNonDeletedType()));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findAll_withNotDeletedSpec_excludesDeletedClients() {
        clientRepository.saveAllAndFlush(List.of(
                buildClient(100L, anyNonDeletedType()),
                buildClient(200L, ClientType.DELETED),
                buildClient(300L, anyNonDeletedType())
        ));

        Page<Client> result = clientRepository.findAll(
                ClientSpecifications.notDeleted(),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Client::getMdmCode)
                .containsExactlyInAnyOrder(100L, 300L);
    }

    @Test
    void findAll_withCombinedSpecs_appliesAllFilters() {
        clientRepository.saveAllAndFlush(List.of(
                buildClient(100L, anyNonDeletedType(), "Ivanov", "Petr"),
                buildClient(200L, anyNonDeletedType(), "Ivanov", "Ivan"),
                buildClient(400L, ClientType.DELETED, "Ivanov", "Ivan")
        ));

        Page<Client> result = clientRepository.findAll(
                ClientSpecifications.notDeleted()
                        .and(ClientSpecifications.lastNameContains("ivan"))
                        .and(ClientSpecifications.hasMdmCode(200L)),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Client::getMdmCode)
                .containsExactly(200L);
    }

    private static Client buildClient(Long mdmCode, ClientType type) {
        return buildClient(mdmCode, type, "Ivanov", "Ivan");
    }

    private static Client buildClient(Long mdmCode, ClientType type,
                                      String lastName, String firstName) {
        Client c = new Client();
        c.setMdmCode(mdmCode);
        c.setClientType(type);
        c.setLastName(lastName);
        c.setFirstName(firstName);
        return c;
    }

    private static ClientType anyNonDeletedType() {
        return Arrays.stream(ClientType.values())
                .filter(t -> t != ClientType.DELETED)
                .findFirst()
                .orElseThrow();
    }
}