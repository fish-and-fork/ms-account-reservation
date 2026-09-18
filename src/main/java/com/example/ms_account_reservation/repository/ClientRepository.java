package com.example.ms_account_reservation.repository;

import com.example.ms_account_reservation.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
}
