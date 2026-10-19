package com.example.ms_account_reservation.repository;

import com.example.ms_account_reservation.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ClientRepository extends
        JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {

    boolean existsByMdmCode (Long mdmCode);
}
