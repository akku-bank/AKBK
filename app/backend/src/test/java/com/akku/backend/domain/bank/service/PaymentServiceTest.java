package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.bank.dto.OfflinePaymentTokenResponse;
import com.akku.backend.domain.bank.dto.PaymentApprovalRequest;
import com.akku.backend.domain.bank.dto.PaymentApprovalResponse;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.entity.Merchant;
import com.akku.backend.domain.bank.entity.OfflinePaymentToken;
import com.akku.backend.domain.bank.entity.Transaction;
import com.akku.backend.domain.bank.exception.BankErrorCode;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.bank.repository.MerchantRepository;
import com.akku.backend.domain.bank.repository.OfflinePaymentTokenRepository;
import com.akku.backend.domain.bank.repository.TransactionRepository;
import com.akku.backend.domain.jelling.entity.Jelling;
import com.akku.backend.domain.jelling.entity.JellingTransaction;
import com.akku.backend.domain.jelling.repository.JellingRepository;
import com.akku.backend.domain.jelling.repository.JellingTransactionRepository;
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OfflinePaymentTokenRepository offlinePaymentTokenRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private JellingRepository jellingRepository;

    @Mock
    private JellingTransactionRepository jellingTransactionRepository;

    private static final String SYSTEM_API_KEY = "system_secret_key_123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "systemApiKey", SYSTEM_API_KEY);
    }

    @Nested
    @DisplayName("오프라인 결제 토큰 발급 (QR 생성)")
    class IssueTokenTests {

        @Test
        @DisplayName("토큰 발급 - 성공 (자녀 계정)")
        void issueToken_Success() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).role("CHILD").build();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(accountRepository.findByUserIdAndType(userId, "CASH")).willReturn(Optional.of(mock(Account.class)));

            OfflinePaymentTokenResponse response = paymentService.issueOfflinePaymentToken(userId);

            assertNotNull(response.getQrToken());
            assertEquals(12, response.getQrToken().length());
            assertEquals(180, response.getExpiresIn());
            verify(offlinePaymentTokenRepository).save(any(OfflinePaymentToken.class));
        }

        @Test
        @DisplayName("토큰 발급 - 실패 (부모 계정 권한 거부)")
        void issueToken_Fail_NotChild() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).role("PARENT").build();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            assertThrows(ApiException.class, () -> paymentService.issueOfflinePaymentToken(userId));
        }
    }

    @Nested
    @DisplayName("오프라인 결제 승인 및 캐시백")
    class ApprovePaymentTests {

        @Test
        @DisplayName("결제 승인 - 성공 (5% 캐시백 포함)")
        void approvePayment_Success() {
            // Given
            String qrToken = "TESTTOKEN123";
            Long amount = 10000L;
            UUID childId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();

            PaymentApprovalRequest request = PaymentApprovalRequest.builder()
                    .qrToken(qrToken)
                    .amount(amount)
                    .merchantName("CU 대전점")
                    .category("CONVENIENCE")
                    .build();

            OfflinePaymentToken token = OfflinePaymentToken.builder()
                    .userId(childId)
                    .token(qrToken)
                    .expiredAt(LocalDateTime.now().plusMinutes(3))
                    .isUsed(false)
                    .build();

            User child = User.builder().id(childId).role("CHILD").build();
            Account account = Account.builder().id(accountId).userId(childId).balance(20000L).build();
            Merchant merchant = Merchant.builder().merchantId(123L).merchantName("CU 대전점").subCategoryName("CONVENIENCE").build();
            Transaction transaction = Transaction.builder().id(UUID.randomUUID()).amount(amount).build();
            Jelling jelling = Jelling.builder().userId(childId).balance(0L).build();

            given(offlinePaymentTokenRepository.findByToken(qrToken)).willReturn(Optional.of(token));
            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            given(accountRepository.findByUserIdAndType(childId, "CASH")).willReturn(Optional.of(account));
            given(merchantRepository.findByMerchantName(anyString())).willReturn(Optional.of(merchant));
            given(transactionRepository.save(any(Transaction.class))).willReturn(transaction);
            given(jellingRepository.findById(childId)).willReturn(Optional.of(jelling));

            // When
            PaymentApprovalResponse response = paymentService.approvePayment(request, SYSTEM_API_KEY);

            // Then
            assertEquals(amount, response.getApprovedAmount());
            assertEquals(10000L, account.getBalance()); // 20000 - 10000
            assertEquals(500L, jelling.getBalance()); // 10000 * 0.05
            assertTrue(token.isUsed());

            verify(accountRepository).save(account);
            verify(jellingRepository).save(jelling);
            verify(jellingTransactionRepository).save(any(JellingTransaction.class));
            verify(offlinePaymentTokenRepository).save(token);
        }

        @Test
        @DisplayName("결제 승인 - 실패 (만료된 토큰)")
        void approvePayment_Fail_ExpiredToken() {
            String qrToken = "EXPIREDTOKEN";
            PaymentApprovalRequest request = PaymentApprovalRequest.builder().qrToken(qrToken).amount(100L).build();
            OfflinePaymentToken token = OfflinePaymentToken.builder()
                    .token(qrToken)
                    .expiredAt(LocalDateTime.now().minusSeconds(1))
                    .isUsed(false)
                    .build();

            given(offlinePaymentTokenRepository.findByToken(qrToken)).willReturn(Optional.of(token));

            ApiException exception = assertThrows(ApiException.class, () -> paymentService.approvePayment(request, SYSTEM_API_KEY));
            assertEquals(BankErrorCode.EXPIRED_PAYMENT_TOKEN, exception.getErrorCode());
        }

        @Test
        @DisplayName("결제 승인 - 실패 (이미 사용된 토큰)")
        void approvePayment_Fail_UsedToken() {
            String qrToken = "USEDTOKEN";
            PaymentApprovalRequest request = PaymentApprovalRequest.builder().qrToken(qrToken).amount(100L).build();
            OfflinePaymentToken token = OfflinePaymentToken.builder()
                    .token(qrToken)
                    .expiredAt(LocalDateTime.now().plusMinutes(5))
                    .isUsed(true)
                    .build();

            given(offlinePaymentTokenRepository.findByToken(qrToken)).willReturn(Optional.of(token));

            ApiException exception = assertThrows(ApiException.class, () -> paymentService.approvePayment(request, SYSTEM_API_KEY));
            assertEquals(BankErrorCode.INVALID_PAYMENT_TOKEN, exception.getErrorCode());
        }

        @Test
        @DisplayName("결제 승인 - 실패 (잔액 부족)")
        void approvePayment_Fail_InsufficientBalance() {
            String qrToken = "TOKEN";
            Long amount = 50000L;
            UUID childId = UUID.randomUUID();
            PaymentApprovalRequest request = PaymentApprovalRequest.builder().qrToken(qrToken).amount(amount).build();
            OfflinePaymentToken token = OfflinePaymentToken.builder().userId(childId).token(qrToken).expiredAt(LocalDateTime.now().plusMinutes(5)).build();
            User child = User.builder().id(childId).build();
            Account account = Account.builder().userId(childId).balance(1000L).build();

            given(offlinePaymentTokenRepository.findByToken(qrToken)).willReturn(Optional.of(token));
            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            given(accountRepository.findByUserIdAndType(childId, "CASH")).willReturn(Optional.of(account));

            ApiException exception = assertThrows(ApiException.class, () -> paymentService.approvePayment(request, SYSTEM_API_KEY));
            assertEquals(BankErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
        }

        @Test
        @DisplayName("결제 승인 - 실패 (잘못된 API Key)")
        void approvePayment_Fail_InvalidApiKey() {
            PaymentApprovalRequest request = PaymentApprovalRequest.builder().build();
            assertThrows(ApiException.class, () -> paymentService.approvePayment(request, "WRONG_KEY"));
        }
    }
}
