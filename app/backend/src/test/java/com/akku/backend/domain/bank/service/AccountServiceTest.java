package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.global.finance.dto.*;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.domain.bank.exception.BankErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("계좌 생성 및 연동")
    class AccountRegistrationTests {
        @Test
        @DisplayName("1. 계좌 생성 - 성공")
        void createAccount_Success() {
            UUID parentId = UUID.randomUUID();
            UUID childId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();
            User parent = User.builder().id(parentId).role("PARENT").familyId(familyId).build();
            User child = User.builder().id(childId).userKey("child-key").role("CHILD").familyId(familyId).build();
            
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));
            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            
            FinanceAccountCreateResponse.Rec mockRec = new FinanceAccountCreateResponse.Rec("001", "12345", null);
            given(ssafyFinanceService.createAccount(anyString(), anyString())).willReturn(mockRec);
            
            Account savedAccount = Account.builder().id(UUID.randomUUID()).balance(0L).build();
            given(accountRepository.save(any(Account.class))).willReturn(savedAccount);

            AccountCreateResponse response = accountService.createAccount(parentId, new AccountCreateRequest(childId));
            assertNotNull(response.accountId());
        }
    }

    @Nested
    @DisplayName("계좌 목록 조회")
    class AccountInquiryTests {
        @Test
        @DisplayName("내 계좌 목록 조회 - 성공")
        void getMyAccounts_Success() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).userKey("my-key").build();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            
            FinanceAccountListResponse.AccountDetails mockAccount = new FinanceAccountListResponse.AccountDetails(
                "001", "한국은행", "사용자", "12345", "보통예금", "1", "수시입출금", "20240401", "20290401", 1000000L, 2000000L, 1000L, "20240323", "KRW"
            );
            given(ssafyFinanceService.getAccounts("my-key")).willReturn(List.of(mockAccount));
            given(accountRepository.findAllByUserId(userId)).willReturn(List.of());

            AccountListResponse response = accountService.getMyAccounts(userId);
            assertEquals(1, response.accounts().size());
            assertEquals("싸피은행", response.accounts().get(0).bankName());
        }

        @Test
        @DisplayName("주계좌 잔액 조회 - 성공")
        void getPrimaryAccountBalance_Success() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).userKey("sync-key").build();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            Account primary = Account.builder().userId(userId).accountNumber("55555").bankCode("001").balance(100L).type("EXTERNAL").isPrimary(true).build();
            given(accountRepository.findByUserIdAndIsPrimaryTrue(userId)).willReturn(Optional.of(primary));

            FinanceAccountListResponse.AccountDetails finAcc = new FinanceAccountListResponse.AccountDetails(
                "001", "한국은행", "사용자", "55555", "계좌", "1", "입출금", "20240401", "20241231", 1000000L, 1000000L, 77700L, "20240401", "KRW"
            );
            given(ssafyFinanceService.getAccounts("sync-key")).willReturn(List.of(finAcc));

            long balance = accountService.getPrimaryAccountBalance(userId);
            assertEquals(77700L, balance);
        }
    }

    @Nested
    @DisplayName("계좌 이체")
    class AccountTransferTests {
        @Test
        @DisplayName("계좌 이체 - 성공")
        void transfer_Success() {
            UUID userId = UUID.randomUUID();
            UUID withdrawalId = UUID.randomUUID();
            TransferRequest request = new TransferRequest(withdrawalId.toString(), "002", "22222", "받는분", 10000L, "123456");
            
            User user = User.builder().id(userId).name("임싸피").userKey("user-key").pinPassword("encoded-pin").build();
            Account withdrawalAcc = Account.builder().id(withdrawalId).userId(userId).accountNumber("11111").bankCode("001").balance(50000L).type("EXTERNAL").build();
            
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(eq("123456"), anyString())).willReturn(true);
            given(accountRepository.findById(withdrawalId)).willReturn(Optional.of(withdrawalAcc));
            given(accountRepository.findByAccountNumberAndBankCode(anyString(), anyString())).willReturn(Optional.empty()); // 외부 이체
            
            FinanceAccountListResponse.AccountDetails details = new FinanceAccountListResponse.AccountDetails(
                "001", "한국은행", "사용자", "11111", "계좌", "1", "입출금", "20240401", "20241231", 1000000L, 1000000L, 50000L, "20240401", "KRW"
            );
            given(ssafyFinanceService.getAccounts(anyString())).willReturn(List.of(details));
            
            FinanceTransferResponse.Rec mockRes = new FinanceTransferResponse.Rec("tx-123", "20240326", "TRANS", "이체", "22222");
            // 8-arg transfer stubbing
            given(ssafyFinanceService.transfer(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyString())).willReturn(mockRes);

            TransferResponse res = accountService.transfer(userId, request);
            assertNotNull(res.transactionId());
            assertEquals(40000L, res.remainBalance());
        }
    }

    @Nested
    @DisplayName("계좌 실명 조회")
    class AccountHolderInquiryTests {
        @Test
        @DisplayName("실명 조회 - 성공")
        void getAccountHolderName_Success() {
            String bankCode = "001";
            String accountNo = "12345";
            UUID userId = UUID.randomUUID();
            
            Account account = Account.builder().userId(userId).accountNumber(accountNo).bankCode(bankCode).build();
            User user = User.builder().id(userId).name("김싸피").build();
            
            given(accountRepository.findByAccountNumberAndBankCode(accountNo, bankCode)).willReturn(Optional.of(account));
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            String result = accountService.getAccountHolderName(bankCode, accountNo);
            assertEquals("김싸피", result);
        }
    }
}
