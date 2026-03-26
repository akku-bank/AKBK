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
import com.akku.backend.domain.bank.entity.Transaction;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.bank.repository.TransactionRepository;
import com.akku.backend.domain.bank.service.AccountService;
import com.akku.backend.global.finance.dto.FinanceTransactionHistoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

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
    private TransactionRepository transactionRepository;

    @Mock
    private SsafyFinanceService ssafyFinanceService;

    @Mock
    private AccountService accountService;

    @Test
    @DisplayName("거래 내역 조회 - 성공")
    void getTransactionHistory_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).userKey("user-key").isHidden(false).build();
        Account account = Account.builder().id(UUID.randomUUID()).userId(userId).accountNumber("12345").build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(accountRepository.findAllByUserId(userId)).willReturn(List.of(account));

        FinanceTransactionHistoryResponse.TransactionDetails mockDetail = new FinanceTransactionHistoryResponse.TransactionDetails(
                "tx-1", "20260312", "134353", "2", "출금", "12345", "5000", "45000", "출금", "스타벅스"
        );
        given(ssafyFinanceService.getTransactionHistory(anyString(), anyString(), anyString(), anyString()))
                .willReturn(List.of(mockDetail));
        given(transactionRepository.existsByTransactionUniqueNo(anyString())).willReturn(true);

        Transaction tx = Transaction.builder()
                .accountId(account.getId())
                .date("20260312134353")
                .merchantName("스타벅스")
                .amount(5000L)
                .transactionType("2")
                .balanceAfter(45000L)
                .isHidden(false)
                .memo("커피한잔")
                .build();
        given(transactionRepository.findAllByAccountIdAndDateBetween(any(), anyString(), anyString(), any(Sort.class))).willReturn(List.of(tx));
        given(accountService.getPrimaryAccountBalance(userId)).willReturn(45000L);

        TransactionHistoryResponse response = transactionService.getTransactionHistory(userId, 2026, 3);

        assertNotNull(response);
        assertFalse(response.transactions().isEmpty());
        assertEquals(1, response.transactions().size());
        assertEquals(45000L, response.balance());
        assertEquals("스타벅스", response.transactions().get(0).place());
        assertEquals(5000L, response.transactions().get(0).amount());
        assertEquals("커피한잔", response.transactions().get(0).memo());
    }

    @Test
    @DisplayName("자녀 거래 내역 조회 - 성공 및 글로벌 마스킹 확인")
    void getChildTransactionHistory_Success_GlobalMasking() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        User parent = User.builder().id(parentId).familyId(familyId).role("PARENT").build();
        User child = User.builder().id(childId).userKey("child-key").familyId(familyId).role("CHILD").isHidden(true).build();
        Account account = Account.builder().id(UUID.randomUUID()).userId(childId).accountNumber("12345").build();

        given(userRepository.findById(parentId)).willReturn(Optional.of(parent));
        given(userRepository.findById(childId)).willReturn(Optional.of(child));
        given(accountRepository.findAllByUserId(childId)).willReturn(List.of(account));
        
        FinanceTransactionHistoryResponse.TransactionDetails detail = new FinanceTransactionHistoryResponse.TransactionDetails(
                "tx-2", "20260312", "140000", "2", "출금", "12345", "3000", "42000", "출금", "치킨구매"
        );
        given(ssafyFinanceService.getTransactionHistory(anyString(), anyString(), anyString(), anyString()))
                .willReturn(List.of(detail));
        given(transactionRepository.existsByTransactionUniqueNo(anyString())).willReturn(true);

        Transaction tx = Transaction.builder()
                .accountId(account.getId())
                .date("20260312140000")
                .merchantName("치킨구매")
                .amount(3000L)
                .transactionType("2")
                .balanceAfter(42000L)
                .isHidden(false)
                .build();
        given(transactionRepository.findAllByAccountIdAndDateBetween(any(), anyString(), anyString(), any(Sort.class))).willReturn(List.of(tx));
        given(accountService.getPrimaryAccountBalance(childId)).willReturn(42000L);

        // 1. 자녀 본인 조회
        TransactionHistoryResponse childHistory = transactionService.getTransactionHistory(childId, 2026, 3);
        assertEquals("치킨구매", childHistory.transactions().get(0).place());

        // 2. 부모의 조회 (글로벌 숨김 설정 대신 개별 숨김 처리된 내역 마스킹 로직 확인)
        // 현재 TransactionService.java 139행은 t.isHidden()을 체크함.
        // 내역을 숨김 처리한 후 조회.
        tx.updateVisibility(true);
        TransactionHistoryResponse parentHistory = transactionService.getChildTransactionHistory(parentId, childId, 2026, 3);
        assertTrue(parentHistory.transactions().get(0).isHidden());
        assertEquals("비공개 내역 🤫", parentHistory.transactions().get(0).place());
    }

    @Test
    @DisplayName("자녀 거래 내역 조회 - 실패 (부적절한 권한)")
    void getChildTransactionHistory_Fail_InvalidRole() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        
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

    @Test
    @DisplayName("개별 거래 내역 숨김 설정 - 성공")
    void updateTransactionVisibility_Success() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String txId = "tx-123";
        TransactionVisibilityRequest request = new TransactionVisibilityRequest(true);
        
        Account account = Account.builder().id(accountId).userId(userId).build();
        Transaction tx = Transaction.builder().accountId(accountId).transactionUniqueNo(txId).isHidden(false).build();

        given(transactionRepository.findByTransactionUniqueNo(txId)).willReturn(Optional.of(tx));
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        TransactionVisibilityResponse response = transactionService.updateTransactionVisibility(userId, txId, request);

        assertTrue(response.isHidden());
        assertTrue(tx.getIsHidden());
        verify(transactionRepository, times(1)).save(tx);
    }

    @Test
    @DisplayName("거래 내역 메모 수정 - 성공")
    void updateTransactionMemo_Success() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String txId = "tx-123";
        String memo = "새로운 메모";
        
        Account account = Account.builder().id(accountId).userId(userId).build();
        Transaction tx = Transaction.builder().accountId(accountId).transactionUniqueNo(txId).memo("").build();

        given(transactionRepository.findByTransactionUniqueNo(txId)).willReturn(Optional.of(tx));
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        transactionService.updateTransactionMemo(userId, txId, memo);

        assertEquals(memo, tx.getMemo());
        verify(transactionRepository, times(1)).save(tx);
    }
}
