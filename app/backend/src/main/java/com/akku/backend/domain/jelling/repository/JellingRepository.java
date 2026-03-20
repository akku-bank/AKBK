package com.akku.backend.domain.jelling.repository;

import com.akku.backend.domain.jelling.entity.Jelling;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JellingRepository extends JpaRepository<Jelling, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Jelling j where j.userId = :userId")
    Optional<Jelling> findByUserIdWithLock(@Param("userId") UUID userId);
}
