package com.example.ms_account_reservation.repository;

import com.example.ms_account_reservation.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountStatusRepository extends JpaRepository<AccountStatus, Integer> {
}
