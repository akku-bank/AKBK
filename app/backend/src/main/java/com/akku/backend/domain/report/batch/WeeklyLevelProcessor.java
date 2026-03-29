package com.akku.backend.domain.report.batch;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.quiz.repository.UserQuizRepository;
import com.akku.backend.domain.report.entity.WeeklyReport;
import com.akku.backend.domain.report.repository.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyLevelProcessor implements ItemProcessor<User, User> {

    private final WeeklyReportRepository weeklyReportRepository;
    private final AccountRepository accountRepository;
    private final UserQuizRepository userQuizRepository;
    private final Clock clock;

    @Override
    public User process(User user) {
        // 자녀인 경우만 처리
        if (!"CHILD".equals(user.getRole())) {
            return null;
        }

        // 정산 기간 (지난주 월요일 ~ 일요일)
        LocalDate now = LocalDate.now(clock);
        LocalDate lastMonday = now.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSunday = lastMonday.plusDays(6);

        // 주간 리포트 데이터 조회 (수입/지출 합계)
        List<WeeklyReport> reports = weeklyReportRepository.findByIdUserIdAndIdStartDay(user.getId(), lastMonday);
        long income = reports.stream()
                .filter(r -> "INCOME".equals(r.getId().getType()))
                .mapToLong(WeeklyReport::getTotalAmount)
                .findFirst().orElse(0L);
        long spend = reports.stream()
                .filter(r -> "SPEND".equals(r.getId().getType()))
                .mapToLong(WeeklyReport::getTotalAmount)
                .findFirst().orElse(0L);

        // 계좌 잔액 및 이월금(주초 잔액) 계산
        Account cashAccount = accountRepository.findByUserIdAndType(user.getId(), "CASH").orElse(null);
        long currentBalance = (cashAccount != null) ? cashAccount.getBalance() : 0L;

        // DB에 기록된 기초 잔액을 최우선으로 사용, 없으면 역산 방식 병행
        long carryover = reports.stream()
                .map(WeeklyReport::getStartBalance)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> Math.max(0, currentBalance - (income - spend)));

        long totalFunds = income + carryover;

        // 이번 주(오늘 시작하는 주)의 리포트에 기초 잔액 미리 기록 (다음 주 정산 시 활용)
        saveNextWeekStartBalance(user, now, currentBalance);

        // 영역별 점수 계산
        int spendingScore = calculateSpendingScore(spend, totalFunds);
        int balanceScore = calculateBalanceScore(currentBalance, totalFunds);
        int quizScore = calculateQuizScore(user.getId(), lastMonday, lastSunday);

        // 최종 점수 및 레벨 산출 (100점 만점)
        int totalScore = Math.min(100, spendingScore + balanceScore + quizScore);
        int level = calculateLevel(totalScore);

        // 유저 정보 업데이트
        user.updateLevelAndScore(level, totalScore);
        
        log.info("[Batch] User {} - Score: {}, Level: {} (Spending: {}, Balance: {}, Quiz: {})", 
                user.getId(), totalScore, level, spendingScore, balanceScore, quizScore);
        
        return user;
    }

    /**
     * 지출 적정성 (30점 만점)
     * 지출 비율 0~50%: 30점 / 51~80%: 20점 / 81~100%: 10점 / 100% 초과: 0점
     */
    private int calculateSpendingScore(long spend, long totalFunds) {
        if (totalFunds <= 0) return 0; // 가용 자금이 없으면 평가 불가
        double ratio = (double) spend / totalFunds;
        if (ratio <= 0.5) return 30;
        if (ratio <= 0.8) return 20;
        if (ratio <= 1.0) return 10;
        return 0;
    }

    /**
     * 잔액 유지력 (20점 만점)
     * 공식: 20 * (현재잔액 / 총 자금(입금액 + 이월금))
     */
    private int calculateBalanceScore(long currentBalance, long totalFunds) {
        if (totalFunds <= 0) return 0;
        double ratio = (double) currentBalance / totalFunds;
        return (int) Math.min(20, Math.round(20 * ratio));
    }

    /**
     * 퀴즈 성과 (50점 만점)
     * 문제당 5점, 7개 모두 정답 시 50점 (보너스 +15점)
     */
    private int calculateQuizScore(java.util.UUID userId, LocalDate start, LocalDate end) {
        long correctCount = userQuizRepository.countByUserIdAndIsCorrectTrueAndSolvedDateBetween(userId, start, end);
        if (correctCount >= 7) return 50;
        return (int) (correctCount * 5);
    }

    /**
     * 레벨링 (1~5단계)
     */
    private int calculateLevel(int score) {
        if (score <= 20) return 1;
        if (score <= 40) return 2;
        if (score <= 60) return 3;
        if (score <= 80) return 4;
        return 5;
    }

    /**
     * 다음 주 정산을 위해 오늘 시점의 잔액을 기초 잔액(이월금)으로 미리 저장합니다.
     */
    private void saveNextWeekStartBalance(User user, LocalDate today, long balance) {
        com.akku.backend.domain.report.entity.WeeklyReportId id = 
                new com.akku.backend.domain.report.entity.WeeklyReportId(user.getId(), today, "INCOME");
        
        WeeklyReport report = weeklyReportRepository.findById(id)
                .orElseGet(() -> WeeklyReport.builder()
                        .id(id)
                        .user(user)
                        .mon(0L).tue(0L).wed(0L).thu(0L).fri(0L).sat(0L).sun(0L)
                        .totalAmount(0L)
                        .build());
        
        report.updateStartBalance(balance);
        weeklyReportRepository.save(report);
    }
}
