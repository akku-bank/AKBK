package com.akku.backend.domain.bank.repository;

import com.akku.backend.domain.bank.entity.Transaction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    Optional<Transaction> findByTransactionUniqueNo(String transactionUniqueNo);
    
    List<Transaction> findAllByAccountIdAndDateBetween(UUID accountId, String startDate, String endDate, Sort sort);
    
    boolean existsByTransactionUniqueNo(String transactionUniqueNo);
}
