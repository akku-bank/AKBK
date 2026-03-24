package com.akku.backend.domain.challenge.repository;

import com.akku.backend.domain.challenge.entity.EsgChallenge;
import com.akku.backend.domain.challenge.entity.EsgChallengeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface EsgChallengeRepository extends JpaRepository<EsgChallenge, UUID> {

    // Lazy Insert 조회 — 해당 유저의 이번 주 챌린지 조회
    Optional<EsgChallenge> findByUserIdAndStartDate(UUID userId, LocalDate startDate);

    // 이벤트 리스너용 — IN_PROGRESS 상태인 이번 주 챌린지 조회
    @Query("SELECT e FROM EsgChallenge e " +
            "WHERE e.user.id = :userId " +
            "AND e.startDate = :startDate " +
            "AND e.status = :status")
    Optional<EsgChallenge> findByUserIdAndStartDateAndStatus(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("status") EsgChallengeStatus status
    );

    // 보상 수령 동시성 제어용 — SELECT ... FOR UPDATE (비관적 쓰기 락)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EsgChallenge e WHERE e.id = :id")
    Optional<EsgChallenge> findByIdForUpdate(@Param("id") UUID id);
}
