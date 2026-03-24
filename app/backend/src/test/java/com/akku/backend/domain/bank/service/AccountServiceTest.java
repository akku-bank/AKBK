package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.global.finance.dto.FinanceAccountAuthCheckResponse;
import com.akku.backend.global.finance.dto.FinanceAccountAuthResponse;
import com.akku.backend.global.finance.dto.FinanceAccountCreateResponse;
import com.akku.backend.global.finance.dto.FinanceAccountListResponse;
import com.akku.backend.global.finance.dto.FinanceTransferResponse;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.domain.bank.exception.BankErrorCode;
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

    @Mock
    private com.akku.backend.domain.bank.repository.AccountVerificationRepository accountVerificationRepository;

    @Nested
    @DisplayName("계좌 생성 및 연동")
    class AccountRegistrationTests {
        @Test
        @DisplayName("1. 계좌 생성 - 성공 (부모가 자녀 계좌 생성)")
        void createAccount_Success() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();
            AccountCreateRequest request = new AccountCreateRequest(childId);
            User parent = User.builder().id(parentId).role("PARENT").familyId(familyId).build();
            User child = User.builder().id(childId).userKey("child-key").role("CHILD").familyId(familyId).build();
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
            verify(ssafyFinanceService).createAccount(eq("child-key"), eq("001-1-85a431ad30cd43"));
        }


        @Test
        @DisplayName("4. 1원 송금 인증 요청 - 성공")
        void request1WonVerification_Success() {
            UUID userId = UUID.randomUUID();
            AccountVerifyRequest request = new AccountVerifyRequest("088", "110-123-456789");
            User user = User.builder().id(userId).userKey("user-key").build();
            
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(accountRepository.existsByAccountNumberAndBankCode(anyString(), anyString())).willReturn(false);
            
            accountService.request1WonVerification(userId, request);
            
            verify(ssafyFinanceService).openAccountAuth(eq("user-key"), eq("110-123-456789"), contains("SSAFY"));
        }

        @Test
        @DisplayName("5. 1원 송금 인증 확인 및 연동 - 성공")
        void verifyAccountAndLink_Success() {
            UUID userId = UUID.randomUUID();
            AccountVerifyConfirmRequest request = new AccountVerifyConfirmRequest("088", "110-123-456789", "1234");
            User user = User.builder().id(userId).userKey("user-key").build();
            
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            
            com.akku.backend.domain.bank.entity.AccountVerification verification = com.akku.backend.domain.bank.entity.AccountVerification.builder()
                 .authText("SSAFY")
                 .expiresAt(java.time.LocalDateTime.now().plusMinutes(5))
                 .build();
            given(accountVerificationRepository.findTopByUserIdAndAccountNumberAndBankCodeOrderByCreatedAtDesc(any(), anyString(), anyString()))
                 .willReturn(Optional.of(verification));

            FinanceAccountAuthCheckResponse.Rec mockVerifyRec = new FinanceAccountAuthCheckResponse.Rec(
                "SUCCESS", 12345L, "110-123-456789"
            );
            given(ssafyFinanceService.checkAuthCode(anyString(), anyString(), anyString(), anyString())).willReturn(mockVerifyRec);
            
            Account savedAccount = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountNumber("110-123-456789")
                .bankCode("088")
                .type("EXTERNAL")
                .build();
            given(accountRepository.save(any(Account.class))).willReturn(savedAccount);

            AccountLinkResponse response = accountService.verifyAccountAndLink(userId, request);
            
            assertNotNull(response.accountId());
            assertEquals("신한은행", response.bankName());
            verify(accountRepository).save(any(Account.class));
        }


        @Test
        @DisplayName("4. 1원 송금 인증 요청 - 성공")
        void request1WonVerification_Success() {
            UUID userId = UUID.randomUUID();
            AccountVerifyRequest request = new AccountVerifyRequest("088", "110-123-456789");
            User user = User.builder().id(userId).userKey("user-key").build();
            
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(accountRepository.existsByAccountNumberAndBankCode(anyString(), anyString())).willReturn(false);
            
            accountService.request1WonVerification(userId, request);
            
            verify(ssafyFinanceService).openAccountAuth(eq("user-key"), eq("110-123-456789"), contains("SSAFY"));
        }

        @Test
        @DisplayName("5. 1원 송금 인증 확인 및 연동 - 성공")
        void verifyAccountAndLink_Success() {
            UUID userId = UUID.randomUUID();
            AccountVerifyConfirmRequest request = new AccountVerifyConfirmRequest("088", "110-123-456789", "1234");
            User user = User.builder().id(userId).userKey("user-key").build();
            
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            
            FinanceAccountAuthCheckResponse.Rec mockVerifyRec = new FinanceAccountAuthCheckResponse.Rec(
                "SUCCESS", 12345L, "110-123-456789"
            );
            given(ssafyFinanceService.checkAuthCode(anyString(), anyString(), anyString(), anyString())).willReturn(mockVerifyRec);
            
            Account savedAccount = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountNumber("110-123-456789")
                .bankCode("088")
                .type("EXTERNAL")
                .build();
            given(accountRepository.save(any(Account.class))).willReturn(savedAccount);

            AccountLinkResponse response = accountService.verifyAccountAndLink(userId, request);
            
            assertNotNull(response.accountId());
            assertEquals("AK은행", response.bankName());
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("6. 1원 송금 인증 확인 - 실패 (코드 불일치)")
        void verifyAccountAndLink_Failure_InvalidCode() {
            UUID userId = UUID.randomUUID();
            AccountVerifyConfirmRequest request = new AccountVerifyConfirmRequest("088", "110-123-456789", "9999");
            User user = User.builder().id(userId).userKey("user-key").build();
            
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            
            com.akku.backend.domain.bank.entity.AccountVerification verification = com.akku.backend.domain.bank.entity.AccountVerification.builder()
                 .authText("SSAFY")
                 .expiresAt(java.time.LocalDateTime.now().plusMinutes(5))
                 .build();
            given(accountVerificationRepository.findTopByUserIdAndAccountNumberAndBankCodeOrderByCreatedAtDesc(any(), anyString(), anyString()))
                 .willReturn(Optional.of(verification));

            FinanceAccountAuthCheckResponse.Rec mockVerifyRec = new FinanceAccountAuthCheckResponse.Rec(
                "FAIL", 12345L, "110-123-456789"
            );
            given(ssafyFinanceService.checkAuthCode(anyString(), anyString(), anyString(), anyString())).willReturn(mockVerifyRec);

            ApiException exception = assertThrows(ApiException.class, () -> {
                accountService.verifyAccountAndLink(userId, request);
            });
            
            assertEquals(BankErrorCode.INVALID_AUTH_CODE, exception.getErrorCode());
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
