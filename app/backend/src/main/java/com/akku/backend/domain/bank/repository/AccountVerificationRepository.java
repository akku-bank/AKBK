package com.akku.backend.domain.bank.repository;

import com.akku.backend.domain.bank.entity.AccountVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AccountVerificationRepository extends JpaRepository<AccountVerification, UUID> {
    
    Optional<AccountVerification> findTopByUserIdAndAccountNumberAndBankCodeOrderByCreatedAtDesc(
            UUID userId, String accountNumber, String bankCode);

    void deleteAllByUserIdAndAccountNumberAndBankCode(UUID userId, String accountNumber, String bankCode);

    void deleteAllByExpiresAtBefore(LocalDateTime now);
}
