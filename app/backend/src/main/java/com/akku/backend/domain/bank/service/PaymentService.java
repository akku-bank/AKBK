package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.exception.AuthErrorCode;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.bank.dto.OfflinePaymentTokenResponse;
import com.akku.backend.domain.bank.dto.PaymentApprovalRequest;
import com.akku.backend.domain.bank.dto.PaymentApprovalResponse;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.entity.OfflinePaymentToken;
import com.akku.backend.domain.bank.entity.Merchant;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final OfflinePaymentTokenRepository offlinePaymentTokenRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final JellingRepository jellingRepository;
    private final JellingTransactionRepository jellingTransactionRepository;

    @Value("${ssafy.api.system-key}")
    private String systemApiKey;

    @jakarta.annotation.PostConstruct
    public void validateConfig() {
        if (systemApiKey == null || systemApiKey.trim().isEmpty()) {
            throw new IllegalStateException("필수 설정값 'ssafy.api.system-key'가 누락되었습니다. 환경 변수 'OFFLINE_PAYMENT_SYSTEM_KEY'를 확인해 주세요.");
        }
    }

    @Transactional
    public OfflinePaymentTokenResponse issueOfflinePaymentToken(UUID userId) {
        // 사용자 확인 및 권한 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(AuthErrorCode.UNAUTHORIZED)); // 혹은 적절한 에러

        if (!"CHILD".equals(user.getRole())) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        // 현금 계좌 확인
        accountRepository.findByUserIdAndType(userId, "CASH")
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));

        // 토큰 생성 (3분 만료)
        String tokenString = generateSecureToken();
        int expiresInSeconds = 180;
        
        OfflinePaymentToken token = OfflinePaymentToken.builder()
                .userId(userId)
                .token(tokenString)
                .expiredAt(LocalDateTime.now().plusSeconds(expiresInSeconds))
                .build();

        offlinePaymentTokenRepository.save(token);

        return OfflinePaymentTokenResponse.of(tokenString, expiresInSeconds);
    }

    /**
     * 오프라인 결제 승인
     */
    @Transactional
    public PaymentApprovalResponse approvePayment(PaymentApprovalRequest request, String apiKey) {
        // API Key 검증
        if (systemApiKey == null || !systemApiKey.equals(apiKey)) {
            throw new ApiException(AuthErrorCode.UNAUTHORIZED);
        }

        // 토큰 유효성 및 만료/사용 여부 확인
        OfflinePaymentToken token = offlinePaymentTokenRepository.findByToken(request.getQrToken())
                .orElseThrow(() -> new ApiException(BankErrorCode.INVALID_PAYMENT_TOKEN));

        if (token.isUsed()) {
            throw new ApiException(BankErrorCode.INVALID_PAYMENT_TOKEN);
        }

        if (token.isExpired()) {
            throw new ApiException(BankErrorCode.EXPIRED_PAYMENT_TOKEN);
        }

        // 자녀 확인 및 계좌 조회
        User child = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(AuthErrorCode.ACCESS_DENIED));

        Account account = accountRepository.findByUserIdAndTypeWithLock(child.getId(), "CASH")
                .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));

        // 잔액 확인
        if (account.getBalance() < request.getAmount()) {
            throw new ApiException(BankErrorCode.INSUFFICIENT_BALANCE);
        }

        // 가맹점 정보 조회 또는 생성
        Merchant merchant = merchantRepository.findByMerchantName(request.getMerchantName())
                .orElseGet(() -> {
                    // 가입되지 않은 가맹점의 경우 임시 생성
                    return merchantRepository.save(Merchant.builder()
                            .merchantName(request.getMerchantName())
                            .subCategoryName(request.getCategory())
                            .build());
                });

        // 결제 처리 (잔액 차감 및 거래 내역 기록)
        account.deductBalance(request.getAmount());
        accountRepository.save(account);

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .accountId(account.getId())
                .merchantId(merchant.getMerchantId())
                .amount(request.getAmount())
                .transactionType("OFFLINE_PAYMENT")
                .merchantName(merchant.getMerchantName())
                .subCategoryName(merchant.getSubCategoryName())
                .build());

        // 5% 젤링 캐시백 정책 적용
        long cashbackAmount = (long) (request.getAmount() * 0.05);
        if (cashbackAmount > 0) {
            Jelling jelling = jellingRepository.findById(child.getId())
                    .orElseGet(() -> jellingRepository.save(Jelling.builder()
                            .user(child)
                            .balance(0L)
                            .build()));
            
            jelling.addBalance(cashbackAmount);
            jellingRepository.save(jelling);

            jellingTransactionRepository.save(JellingTransaction.builder()
                    .user(child)
                    .amount(cashbackAmount)
                    .type("REWARD")
                    .description("오프라인 결제 캐시백 (5%)")
                    .build());
        }

        // 토큰 사용 처리
        token.use();
        offlinePaymentTokenRepository.save(token);

        return PaymentApprovalResponse.of(transaction.getId(), request.getAmount());
    }

    private String generateSecureToken() {
        // 12자리 대문자/숫자 혼합 토큰 생성
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
