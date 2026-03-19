package com.akku.backend.domain.bank.repository;

import com.akku.backend.domain.bank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** Kafka 이벤트 중복 처리 여부 확인 */
    boolean existsByEventId(UUID eventId);
}
