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
import java.util.Optional;
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
            "AND t.created_at < :endExclusive",
            nativeQuery = true)
    Long calculateCurrentSpending(
            @Param("userId") UUID userId,
            @Param("category") String category,
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive
    );

    // 실시간 FAIL 판정용 — 결제 이벤트 수신 시 진행 중인 챌린지 조회
    // 해당 유저의 특정 카테고리 챌린지 중, 결제일이 챌린지 기간(startDate~endDate) 안에 속하는 IN_PROGRESS 챌린지
    @Query("SELECT sc FROM SpendingChallenge sc " +
            "WHERE sc.user.id = :userId " +
            "AND sc.subCategoryName = :subCategoryName " +
            "AND sc.status = :status " +
            "AND sc.startDate <= :date " +
            "AND sc.endDate >= :date")
    Optional<SpendingChallenge> findActiveChallenge(
            @Param("userId") UUID userId,
            @Param("subCategoryName") String subCategoryName,
            @Param("status") ChallengeStatus status,
            @Param("date") LocalDate date
    );

    // 주간 정산 배치용 — 지난주 IN_PROGRESS 챌린지 일괄 조회 (스케줄러 STEP 1)
    List<SpendingChallenge> findAllByStatusAndEndDate(ChallengeStatus status, LocalDate endDate);

    // 주간 활성화 배치용 — 이번주 APPROVED 챌린지 일괄 조회 (스케줄러 STEP 2)
    List<SpendingChallenge> findAllByStatusAndStartDate(ChallengeStatus status, LocalDate startDate);

    // 미수령 보상 목록 조회용 — 특정 유저의 SUCCESS + 특정 endDate 챌린지 조회
    List<SpendingChallenge> findAllByUserAndStatusAndEndDate(User user, ChallengeStatus status, LocalDate endDate);
}