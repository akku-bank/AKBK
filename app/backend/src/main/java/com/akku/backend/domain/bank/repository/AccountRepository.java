package com.akku.backend.domain.bank.repository;

import com.akku.backend.domain.bank.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findAllByUserId(UUID userId);
}
