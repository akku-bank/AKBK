package com.akku.backend.domain.bank.repository;

import com.akku.backend.domain.bank.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findAllByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdWithLock(UUID id);

    Optional<Account> findByUserIdAndType(UUID userId, String type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.userId = :userId and a.type = :type")
    Optional<Account> findByUserIdAndTypeWithLock(UUID userId, String type);

    Optional<Account> findByUserIdAndIsPrimaryTrue(UUID userId);
    boolean existsByUserId(UUID userId);
    boolean existsByAccountNumberAndBankCode(String accountNumber, String bankCode);
}
