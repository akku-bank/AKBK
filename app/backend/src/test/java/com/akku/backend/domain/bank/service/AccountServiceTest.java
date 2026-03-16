package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.global.finance.dto.FinanceAccountCreateResponse;
import com.akku.backend.global.finance.dto.FinanceAccountListResponse;
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
            AccountCreateRequest request = new AccountCreateRequest(childId, "CASH");
            User parent = User.builder().id(parentId).role("PARENT").build();
            User child = User.builder().id(childId).userKey("child-key").build();
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
        @DisplayName("3. 타행 계좌 연동 - 성공")
        void linkExternalAccount_Success() {
            UUID userId = UUID.randomUUID();
            AccountLinkRequest request = new AccountLinkRequest("004", "1234567890");
            User user = User.builder().id(userId).userKey("user-key").build();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            accountService.linkExternalAccount(userId, request);
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
}
