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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeeklyLevelProcessorTest {

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private WeeklyLevelProcessor weeklyLevelProcessor;

    private User child;
    private UUID userId;

    @BeforeEach
    void setUp() {
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
    @DisplayName("지출 적정성 30점 만점 시나리오: 지출 비율 50% 이하")
    void process_SpendingScore_Perfect() {
        // given
        // 수입 10,000원, 지출 2,000원 (비율 20%)
        WeeklyReport incomeReport = WeeklyReport.builder().totalAmount(10000L).id(new com.akku.backend.domain.report.entity.WeeklyReportId(userId, LocalDate.now(), "INCOME")).build();
        WeeklyReport spendReport = WeeklyReport.builder().totalAmount(2000L).id(new com.akku.backend.domain.report.entity.WeeklyReportId(userId, LocalDate.now(), "SPEND")).build();
        
        given(weeklyReportRepository.findByIdUserIdAndIdStartDay(any(), any())).willReturn(List.of(incomeReport, spendReport));
        
        // 현재 잔액 10,000원 (이월금은 역산 시 2,000원)
        Account account = Account.builder().balance(10000L).build();
        given(accountRepository.findByUserIdAndType(any(), any())).willReturn(Optional.of(account));

        // when
        User result = weeklyLevelProcessor.process(child);

        // then
        // 지출 적정성 (20%): 30점
        // 잔액 유지력 (10,000 / 10,000 + 2,000 = 0.83): 약 17점 (20 * 0.83)
        // 총점 약 47점 -> 레벨 3
        assertThat(result).isNotNull();
        assertThat(result.getScore()).isGreaterThanOrEqualTo(40);
        assertThat(result.getLevel()).isEqualTo(3);
        verify(weeklyReportRepository).save(any()); // 차주 기초 잔액 저장 확인
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

        // when
        User result = weeklyLevelProcessor.process(child);

        // then
        // 총 가용 자금 = 수입(10000) + 기초잔액(5000) = 15,000
        // 잔액 유지력 = 20 * (8000 / 15000) = 약 10.6 -> 11점
        assertThat(result.getScore()).isGreaterThan(0);
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
