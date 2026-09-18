package com.example.ms_account_reservation.repository;

import com.example.ms_account_reservation.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}
