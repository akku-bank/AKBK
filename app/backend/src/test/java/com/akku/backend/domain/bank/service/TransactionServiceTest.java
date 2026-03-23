package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.exception.AuthErrorCode;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.TransactionVisibilityRequest;
import com.akku.backend.domain.bank.dto.TransactionVisibilityResponse;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.domain.bank.dto.TransactionHistoryResponse;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.global.finance.dto.FinanceTransactionHistoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SsafyFinanceService ssafyFinanceService;

    @Test
    @DisplayName("거래 내역 조회 - 성공")
    void getTransactionHistory_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).userKey("user-key").isHidden(false).build();
        Account account = Account.builder().userId(userId).accountNumber("12345").build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(accountRepository.findAllByUserId(userId)).willReturn(List.of(account));

        FinanceTransactionHistoryResponse.TransactionDetails mockDetail = new FinanceTransactionHistoryResponse.TransactionDetails(
                "tx-1", "20260312", "134353", "2", "출금", "12345", "5000", "45000", "스타벅스", "커피"
        );
        given(ssafyFinanceService.getTransactionHistory(anyString(), anyString(), anyString(), anyString()))
                .willReturn(List.of(mockDetail));

        TransactionHistoryResponse response = transactionService.getTransactionHistory(userId, 2026, 3);

        assertNotNull(response);
        assertFalse(response.transactions().isEmpty());
        assertEquals(1, response.transactions().size());
        assertEquals("스타벅스", response.transactions().get(0).merchantName());
        assertEquals(-5000L, response.transactions().get(0).amount());
        assertFalse(response.transactions().get(0).isHidden());
    }

    @Test
    @DisplayName("자녀 거래 내역 조회 - 성공 및 글로벌 마스킹 확인")
    void getChildTransactionHistory_Success_GlobalMasking() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        User parent = User.builder().id(parentId).familyId(familyId).role("PARENT").build();
        // 자녀가 글로벌 숨김 설정을 켠 상태
        User child = User.builder().id(childId).userKey("child-key").familyId(familyId).role("CHILD").isHidden(true).build();
        Account account = Account.builder().userId(childId).accountNumber("12345").build();

        given(userRepository.findById(parentId)).willReturn(Optional.of(parent));
        given(userRepository.findById(childId)).willReturn(Optional.of(child));
        given(accountRepository.findAllByUserId(childId)).willReturn(List.of(account));
        
        FinanceTransactionHistoryResponse.TransactionDetails detail = new FinanceTransactionHistoryResponse.TransactionDetails(
                "tx-2", "20260312", "140000", "2", "출금", "12345", "3000", "42000", "치킨구매", "음식"
        );
        given(ssafyFinanceService.getTransactionHistory(anyString(), anyString(), anyString(), anyString()))
                .willReturn(List.of(detail));

        // 1. 자녀 본인 조회 (숨김 내역도 보임)
        TransactionHistoryResponse childHistory = transactionService.getTransactionHistory(childId, 2026, 3);
        assertTrue(childHistory.transactions().get(0).isHidden());
        assertEquals("치킨구매", childHistory.transactions().get(0).merchantName());

        // 2. 부모의 조회 (글로벌 숨김 설정으로 인해 모든 내역이 마스킹됨)
        TransactionHistoryResponse parentHistory = transactionService.getChildTransactionHistory(parentId, childId, 2026, 3);
        assertTrue(parentHistory.transactions().get(0).isHidden());
        assertEquals("비공개 내역", parentHistory.transactions().get(0).merchantName());
        assertEquals(-3000L, parentHistory.transactions().get(0).amount());
    }

    @Test
    @DisplayName("자녀 거래 내역 조회 - 실패 (부적절한 권한)")
    void getChildTransactionHistory_Fail_InvalidRole() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        
        // 자녀가 부모의 내역을 조회하려는 상황
        User child = User.builder().id(childId).familyId(familyId).role("CHILD").build();
        User parent = User.builder().id(parentId).familyId(familyId).role("PARENT").build();

        given(userRepository.findById(childId)).willReturn(Optional.of(child));
        given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

        ApiException exception = assertThrows(ApiException.class, () ->
                transactionService.getChildTransactionHistory(childId, parentId, 2026, 3));
        
        assertEquals(AuthErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("프라이버시 제어 (전체 숨김 설정) - 성공")
    void updateGlobalVisibility_Success() {
        UUID userId = UUID.randomUUID();
        TransactionVisibilityRequest request = new TransactionVisibilityRequest(true);
        User user = User.builder().id(userId).isHidden(false).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        TransactionVisibilityResponse response = transactionService.updateGlobalVisibility(userId, request);

        assertNotNull(response);
        assertTrue(response.isHidden());
        assertTrue(user.getIsHidden());
        verify(userRepository, times(1)).save(user);
    }
}


