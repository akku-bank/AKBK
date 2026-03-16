package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.global.finance.dto.FinanceAccountCreateResponse;
import com.akku.backend.global.finance.dto.FinanceAccountListResponse;
import com.akku.backend.global.finance.dto.FinanceTransferResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @InjectMocks
    private AccountService accountService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SsafyFinanceService ssafyFinanceService;

    @Nested
    @DisplayName("계좌 생성 및 연동")
    class AccountRegistrationTests {
        @Test
        @DisplayName("1. 계좌 생성 - 성공 (부모가 자녀 계좌 생성)")
        void createAccount_Success() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();
            AccountCreateRequest request = new AccountCreateRequest(childId, "CASH");
            User parent = User.builder().id(parentId).role("PARENT").familyId(familyId).build();
            User child = User.builder().id(childId).userKey("child-key").familyId(familyId).build();
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));
            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            
            FinanceAccountCreateResponse.Rec mockRec = new FinanceAccountCreateResponse.Rec(
                "001", "1234567890", null
            );
            given(ssafyFinanceService.createAccount(anyString(), anyString())).willReturn(mockRec);
            
            Account savedAccount = mock(Account.class);
            given(savedAccount.getId()).willReturn(UUID.randomUUID());
            given(savedAccount.getBalance()).willReturn(0L);
            given(accountRepository.save(any(Account.class))).willReturn(savedAccount);

            AccountCreateResponse response = accountService.createAccount(parentId, request);
            assertNotNull(response.accountId());
            assertEquals(0, response.balance());
            verify(ssafyFinanceService).createAccount(eq("child-key"), eq("CASH"));
        }

        @Test
        @DisplayName("3. 타행 계좌 연동 - 미구현 예외 발생 검증")
        void linkExternalAccount_NotImplemented() {
            UUID userId = UUID.randomUUID();
            AccountLinkRequest request = new AccountLinkRequest("004", "1234567890");
            User user = User.builder().id(userId).userKey("user-key").build();
            
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            
            // 아직 미구현 상태이므로 예외가 발생하는지 검증
            doThrow(new UnsupportedOperationException("아직 구현되지 않은 기능입니다: 타행 계좌 연동"))
                .when(ssafyFinanceService).linkAccount(anyString(), anyString(), anyString());

            assertThrows(UnsupportedOperationException.class, () -> {
                accountService.linkExternalAccount(userId, request);
            });
            
            verify(ssafyFinanceService).linkAccount("user-key", "004", "1234567890");
        }
    }

    @Nested
    @DisplayName("계좌 목록 조회")
    class AccountInquiryTests {
        @Test
        @DisplayName("2. 내 계좌 목록 조회 - 성공")
        void getMyAccounts_Success() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).userKey("my-key").build();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            
            FinanceAccountListResponse.AccountDetails mockAccount = new FinanceAccountListResponse.AccountDetails(
                "001", "한국은행", "사용자", "12345", "보통예금", "1", "수시입출금", "20240401", "20290401", 10000000L, 20000000L, 1000L, "20240323", "KRW"
            );
            given(ssafyFinanceService.getAccounts("my-key")).willReturn(List.of(mockAccount));

            AccountListResponse response = accountService.getMyAccounts(userId);
            assertFalse(response.accounts().isEmpty());
            assertEquals(1, response.accounts().size());
            assertEquals("12345", response.accounts().get(0).accountNumber());
        }
    }

    @Nested
    @DisplayName("계좌 이체")
    class AccountTransferTests {
        @Test
        @DisplayName("1. 계좌 이체 - 성공")
        void transfer_Success() {
            UUID userId = UUID.randomUUID();
            UUID withdrawalAccountId = UUID.randomUUID();
            UUID targetAccountId = UUID.randomUUID();
            TransferRequest request = new TransferRequest(withdrawalAccountId.toString(), targetAccountId.toString(), 10000L, "123456");
            
            User user = User.builder()
                    .id(userId)
                    .userKey("user-key")
                    .pinPassword("123456")
                    .build();
            
            Account withdrawalAccount = Account.builder()
                    .id(withdrawalAccountId)
                    .userId(userId)
                    .accountNumber("11111")
                    .bankCode("001")
                    .balance(50000L)
                    .build();
            
            Account depositAccount = Account.builder()
                    .id(targetAccountId)
                    .accountNumber("22222")
                    .bankCode("002")
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(accountRepository.findById(withdrawalAccountId)).willReturn(Optional.of(withdrawalAccount));
            given(accountRepository.findById(targetAccountId)).willReturn(Optional.of(depositAccount));
            
            FinanceTransferResponse.Rec mockFinRec = new FinanceTransferResponse.Rec(
                "tx-123", "20240323", "TRANSFER", "이체", "22222"
            );
            given(ssafyFinanceService.transfer(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong())).willReturn(mockFinRec);

            TransferResponse response = accountService.transfer(userId, request);

            assertNotNull(response.transactionId());
            assertEquals(40000L, response.remainBalance());
            verify(ssafyFinanceService).transfer(eq("user-key"), eq("001"), eq("11111"), eq("002"), eq("22222"), eq(10000L));
        }
    }
}
