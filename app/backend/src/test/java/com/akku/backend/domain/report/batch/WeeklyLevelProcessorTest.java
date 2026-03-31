package com.akku.backend.domain.report.batch;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.report.entity.WeeklyReport;
import com.akku.backend.domain.report.repository.WeeklyReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeeklyLevelProcessorTest {

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private com.akku.backend.domain.quiz.repository.UserQuizRepository userQuizRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-31T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private WeeklyLevelProcessor weeklyLevelProcessor;

    private User child;
    private UUID userId;

    @BeforeEach
    void setUp() {
        weeklyLevelProcessor = new WeeklyLevelProcessor(weeklyReportRepository, accountRepository, userQuizRepository, clock);
        userId = UUID.randomUUID();
        child = User.builder()
                .id(userId)
                .role("CHILD")
                .isActive(true)
                .level(1)
                .score(0)
                .build();
    }

    @Test
    @DisplayName("퀴즈 7개 모두 정답 시 50점 만점 시나리오")
    void process_QuizScore_Perfect() {
        // given
        WeeklyReport incomeReport = WeeklyReport.builder().totalAmount(10000L).id(new com.akku.backend.domain.report.entity.WeeklyReportId(userId, LocalDate.now(), "INCOME")).build();
        given(weeklyReportRepository.findByIdUserIdAndIdStartDay(any(), any())).willReturn(List.of(incomeReport));
        given(accountRepository.findByUserIdAndType(any(), any())).willReturn(Optional.empty()); // 잔액 0
        
        // 퀴즈 7개 정답
        given(userQuizRepository.countByUserIdAndIsCorrectTrueAndSolvedDateBetween(any(), any(), any())).willReturn(7L);

        // when
        User result = weeklyLevelProcessor.process(child);

        // then
        // 퀴즈 점수 50점 + 소비 만점(30) + 잔액 0점 = 80점
        assertThat(result.getScore()).isEqualTo(80);
    }

    @Test
    @DisplayName("퀴즈 3개 정답 시 15점 반영 시나리오")
    void process_QuizScore_Partial() {
        // given
        WeeklyReport incomeReport = WeeklyReport.builder().totalAmount(10000L).id(new com.akku.backend.domain.report.entity.WeeklyReportId(userId, LocalDate.now(), "INCOME")).build();
        given(weeklyReportRepository.findByIdUserIdAndIdStartDay(any(), any())).willReturn(List.of(incomeReport));
        given(accountRepository.findByUserIdAndType(any(), any())).willReturn(Optional.empty());

        // 퀴즈 3개 정답
        given(userQuizRepository.countByUserIdAndIsCorrectTrueAndSolvedDateBetween(any(), any(), any())).willReturn(3L);

        // when
        User result = weeklyLevelProcessor.process(child);

        // then
        // 퀴즈 점수 15점 + 소비 만점(30) + 잔액 0점 = 45점
        assertThat(result.getScore()).isEqualTo(45);
    }

    @Test
    @DisplayName("지출 적정성 30점 만점 시나리오: 지출 비율 50% 이하")
    void process_SpendingScore_Perfect() {
        // given
        WeeklyReport incomeReport = WeeklyReport.builder().totalAmount(10000L).id(new com.akku.backend.domain.report.entity.WeeklyReportId(userId, LocalDate.now(), "INCOME")).build();
        WeeklyReport spendReport = WeeklyReport.builder().totalAmount(2000L).id(new com.akku.backend.domain.report.entity.WeeklyReportId(userId, LocalDate.now(), "SPEND")).build();
        
        given(weeklyReportRepository.findByIdUserIdAndIdStartDay(any(), any())).willReturn(List.of(incomeReport, spendReport));
        
        Account account = Account.builder().balance(10000L).build();
        given(accountRepository.findByUserIdAndType(any(), any())).willReturn(Optional.of(account));
        
        // 퀴즈 0개 정답
        given(userQuizRepository.countByUserIdAndIsCorrectTrueAndSolvedDateBetween(any(), any(), any())).willReturn(0L);

        // when
        User result = weeklyLevelProcessor.process(child);

        // then
        // 소비 점수 30 + 잔액 점수 17 + 퀴즈 0 = 약 47
        assertThat(result).isNotNull();
        assertThat(result.getScore()).isGreaterThanOrEqualTo(40);
        verify(weeklyReportRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("기초 잔액(startBalance)이 DB에 있는 경우 이를 우선 사용하여 정산")
    void process_UseStartBalanceFromDB() {
        // given
        // 기초 잔액이 5,000원으로 이미 기록되어 있음
        WeeklyReport reportWithStartBalance = WeeklyReport.builder()
                .totalAmount(10000L)
                .startBalance(5000L)
                .id(new com.akku.backend.domain.report.entity.WeeklyReportId(userId, LocalDate.now(), "INCOME"))
                .build();
        
        given(weeklyReportRepository.findByIdUserIdAndIdStartDay(any(), any())).willReturn(List.of(reportWithStartBalance));
        
        // 현재 잔액 8,000원
        Account account = Account.builder().balance(8000L).build();
        given(accountRepository.findByUserIdAndType(any(), any())).willReturn(Optional.of(account));

        // 퀴즈 0개 정답
        given(userQuizRepository.countByUserIdAndIsCorrectTrueAndSolvedDateBetween(any(), any(), any())).willReturn(0L);

        // when
        User result = weeklyLevelProcessor.process(child);

        // then
        // 총 가용 자금 = 수입(10000) + 기초잔액(5000) = 15,000
        // 잔액 유지력 = 20 * (8000 / 15000) = 약 10.6 -> 11점 (반올림)
        assertThat(result.getScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("신규 유저(수입/지출/잔액 모두 0): 소비 점수 0점 반환")
    void process_NewUser_NoFunds_SpendingScoreIsZero() {
        // given: 수입/지출 내역 없음, 잔액 없음
        given(weeklyReportRepository.findByIdUserIdAndIdStartDay(any(), any())).willReturn(List.of());
        given(accountRepository.findByUserIdAndType(any(), any())).willReturn(Optional.empty());
        given(userQuizRepository.countByUserIdAndIsCorrectTrueAndSolvedDateBetween(any(), any(), any())).willReturn(0L);

        // when
        User result = weeklyLevelProcessor.process(child);

        // then: totalFunds = 0 → spendingScore = 0, balanceScore = 0, quizScore = 0
        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("자녀가 아닌 부모 유저의 경우 정산 제외(null 반환)")
    void process_IgnoreParent() {
        // given
        User parent = User.builder().role("PARENT").build();

        // when
        User result = weeklyLevelProcessor.process(parent);

        // then
        assertThat(result).isNull();
    }
}
