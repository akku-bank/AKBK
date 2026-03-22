package com.akku.backend.domain.challenge.repository;

import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import com.akku.backend.domain.challenge.entity.SpendingChallenge;
import com.akku.backend.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SpendingChallengeRepository extends JpaRepository<SpendingChallenge, UUID> {

    // 특정 유저가, 특정 카테고리를, 특정 시작일(다음 주 월요일)에 이미 등록했는지 확인하는 쿼리
    boolean existsByUserAndSubCategoryNameAndStartDate(User user, String subCategoryName, LocalDate startDate);

    // 차주 챌린지 전체 조회 (상태 필터링 없음)
    List<SpendingChallenge> findAllByUserAndStartDate(User user, LocalDate startDate);

    // 차주 챌린지 상태별 조회 (PENDING, APPROVED, REJECTED 등 필터링)
    List<SpendingChallenge> findAllByUserAndStartDateAndStatus(User user, LocalDate startDate, ChallengeStatus status);

    // 특정 유저의 특정 카테고리에 대한 지정된 기간 내의 출금(결제) 합계를 계산하는 쿼리
    @Query(value = "SELECT COALESCE(SUM(t.amount), 0) " +
            "FROM transactions t " +
            "JOIN accounts a ON t.account_id = a.id " +
            "WHERE a.user_id = :userId " +
            "AND t.sub_category_name = :category " +
            "AND t.transaction_type = '출금' " +
            "AND t.created_at >= :start " +
            "AND t.created_at <= :end",
            nativeQuery = true)
    Long calculateCurrentSpending(
            @Param("userId") UUID userId,
            @Param("category") String category,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}