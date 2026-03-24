package com.akku.backend.domain.bank.repository;

import com.akku.backend.domain.bank.entity.OfflinePaymentToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OfflinePaymentTokenRepository extends JpaRepository<OfflinePaymentToken, UUID> {
    Optional<OfflinePaymentToken> findByTokenAndExpiredAtAfter(String token, LocalDateTime now);
    Optional<OfflinePaymentToken> findByToken(String token);
}
