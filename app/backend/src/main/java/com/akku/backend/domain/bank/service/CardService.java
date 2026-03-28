package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.entity.Card;
import com.akku.backend.domain.bank.entity.CardProduct;
import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.exception.BankErrorCode;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.bank.repository.CardProductRepository;
import com.akku.backend.domain.bank.repository.CardRepository;
import com.akku.backend.domain.bank.repository.MerchantRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import com.akku.backend.global.finance.dto.FinanceCardCreateResponse;
import com.akku.backend.global.finance.dto.FinanceCardProductListResponse;
import com.akku.backend.global.finance.dto.FinanceUserCardListResponse;
import com.akku.backend.global.finance.dto.FinanceCardPaymentResponse;
import com.akku.backend.global.finance.dto.FinanceCardTransactionHistoryResponse;
import com.akku.backend.global.finance.dto.FinanceTransferResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardProductRepository cardProductRepository;
    private final CardRepository cardRepository;
    private final MerchantRepository merchantRepository;
    private final AccountRepository accountRepository;
    private final SsafyFinanceService ssafyFinanceService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 카드 상품 목록 조회
     */
    @Transactional
    public List<CardProductResponse> getCardProducts(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 금융망에서 카드 상품 목록 조회
        List<FinanceCardProductListResponse.CardProductDetails> finProducts = ssafyFinanceService.getCardProducts(user.getUserKey());

        // DB 업데이트
        for (FinanceCardProductListResponse.CardProductDetails p : finProducts) {
            cardProductRepository.findByCardUniqueNo(p.cardUniqueNo())
                    .ifPresentOrElse(
                        product -> product.update(p.cardName(), p.cardDescription(), p.baseLimitPerformance(), p.maxBenefitLimit(), toJson(p.cardBenefitInfo())),
                        () -> {
                            CardProduct newProduct = CardProduct.builder()
                                    .cardUniqueNo(p.cardUniqueNo())
                                    .cardIssuerCode(p.cardIssuerCode())
                                    .cardIssuerName(p.cardIssuerName())
                                    .cardName(p.cardName())
                                    .cardTypeCode(p.cardTypeCode())
                                    .cardTypeName(p.cardTypeName())
                                    .baseLimitPerformance(p.baseLimitPerformance())
                                    .maxBenefitLimit(p.maxBenefitLimit())
                                    .cardDescription(p.cardDescription())
                                    .cardBenefitInfo(toJson(p.cardBenefitInfo()))
                                    .build();
                            cardProductRepository.save(newProduct);
                        }
                    );
        }

        // DB 목록 반환
        return cardProductRepository.findAll().stream()
                .map(CardProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 새로운 카드 발급
     */
    @Transactional
    public CardResponse createCard(UUID userId, CardCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        CardProduct product = cardProductRepository.findById(request.cardProductId())
                .orElseThrow(() -> new ApiException(BankErrorCode.CARD_PRODUCT_NOT_FOUND));

        // 카드 생성
        FinanceCardCreateResponse.Rec rec = ssafyFinanceService.createCard(
                user.getUserKey(),
                product.getCardUniqueNo(),
                request.withdrawalAccountNo(),
                request.withdrawalDate()
        );

        Card card = Card.builder()
                .userId(userId)
                .cardProductId(product.getId())
                .cardNo(rec.cardNo())
                .cvc(rec.cvc())
                .cardExpiryDate(rec.cardExpiryDate())
                .withdrawalAccountNo(rec.withdrawalAccountNo())
                .withdrawalDate(rec.withdrawalDate())
                .isActive(true)
                .build();

        Card savedCard = cardRepository.save(card);
        
        return new CardResponse(
            savedCard.getId(),
            savedCard.getCardProductId(),
            savedCard.getCardNo(),
            product.getCardName(),
            product.getCardIssuerName(),
            savedCard.getWithdrawalAccountNo(),
            savedCard.getWithdrawalDate(),
            savedCard.getCardExpiryDate(),
            savedCard.getIsActive()
        );
    }

    /**
     * 내 카드 목록 조회
     */
    @Transactional
    public List<CardResponse> getMyCards(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 금융망에서 내 카드 목록 조회
        List<FinanceUserCardListResponse.UserCardDetails> finCards = ssafyFinanceService.getUserCards(user.getUserKey());

        // DB 업데이트
        for (FinanceUserCardListResponse.UserCardDetails c : finCards) {
            // 카드 상품 정보 조회 및 업데이트/생성
            CardProduct product = cardProductRepository.findByCardUniqueNo(c.cardUniqueNo())
                    .map(p -> {
                        p.update(c.cardName(), c.cardDescription(), c.baseLimitPerformance(), c.maxBenefitLimit(), toJson(c.cardBenefitInfo()));
                        return p;
                    })
                    .orElseGet(() -> cardProductRepository.save(CardProduct.builder()
                            .cardUniqueNo(c.cardUniqueNo())
                            .cardIssuerCode(c.cardIssuerCode())
                            .cardIssuerName(c.cardIssuerName())
                            .cardName(c.cardName())
                            .baseLimitPerformance(c.baseLimitPerformance())
                            .maxBenefitLimit(c.maxBenefitLimit())
                            .cardDescription(c.cardDescription())
                            .cardBenefitInfo(toJson(c.cardBenefitInfo()))
                            .build()));

            // 해당 카드 번호가 이미 있는지 확인
            boolean exists = cardRepository.findAllByUserId(userId).stream()
                    .anyMatch(card -> card.getCardNo().equals(c.cardNo()));

            if (!exists) {
                Card newCard = Card.builder()
                        .userId(userId)
                        .cardProductId(product.getId())
                        .cardNo(c.cardNo())
                        .cvc(c.cvc())
                        .cardExpiryDate(c.cardExpiryDate())
                        .withdrawalAccountNo(c.withdrawalAccountNo())
                        .withdrawalDate(c.withdrawalDate())
                        .isActive(true)
                        .build();

                cardRepository.save(newCard);
            }
        }

        // DB 정보 반환
        return cardRepository.findAllByUserId(userId).stream()
                .map(CardResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카드 결제 처리
     */
    @Transactional
    public void processPayment(UUID userId, CardPaymentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        Card card = cardRepository.findById(request.cardId())
                .orElseThrow(() -> new ApiException(BankErrorCode.CARD_NOT_FOUND));

        if (!card.getUserId().equals(userId)) {
            throw new ApiException(BankErrorCode.CARD_NOT_FOUND);
        }

        // 금융망에 결제 요청
        FinanceCardPaymentResponse.Rec result = ssafyFinanceService.createCardTransaction(
                user.getUserKey(),
                card.getCardNo(),
                card.getCvc(),
                request.merchantId(),
                request.paymentBalance()
        );

        // [체크카드 시뮬레이션] 결제 성공 시 계좌에서 즉시 잔액 차감 수행
        // 카드 연동 계좌(withdrawalAccountNo)에서 가맹점 정산용 더미 계좌로 이체
        String merchantAccountNo = "9990472825549529";
        log.info("💸 [체크카드 시뮬레이션] 잔액 차감 시도: 출금계좌={}, 입금계좌(가맹점)={}, 금액={}", 
                card.getWithdrawalAccountNo(), merchantAccountNo, request.paymentBalance());

        String withdrawalUniqueNo = null;
        try {
            FinanceTransferResponse.Rec transferResult = ssafyFinanceService.transfer(
                    user.getUserKey(),
                    "999", // 사용자 은행 (싸피은행 고정)
                    card.getWithdrawalAccountNo(),
                    "999", // 가맹점 은행 (싸피은행 고정)
                    merchantAccountNo,
                    request.paymentBalance(),
                    "가맹점정산",
                    "카드결제:" + result.merchantName()
            );
            withdrawalUniqueNo = transferResult.transactionUniqueNo();
            log.info("✅ [체크카드 시뮬레이션] 잔액 차감 성공 - withdrawalUniqueNo: {}", withdrawalUniqueNo);
        } catch (Exception e) {
            log.error("❌ [체크카드 시뮬레이션] 잔액 차감 실패 (A1003 원인 확인용): {}", e.getMessage());
            // 결제 자체는 성공했으므로 우선 진행 (내역 저장을 위해 예외를 던지지 않음)
            // 실제 운영 환경이라면 여기서 Rollback 처리를 고민해야 함
        }

        // 로컬 DB에서도 계좌 잔액 차감 수행 (하이픈 제거 정규화 포함)
        String normalizedAccountNo = card.getWithdrawalAccountNo().replace("-", "");
        Account account = accountRepository.findByAccountNumberAndBankCode(normalizedAccountNo, "999")
                .orElseGet(() -> {
                    return accountRepository.findByAccountNumberAndBankCode(card.getWithdrawalAccountNo(), "999")
                            .orElseThrow(() -> new ApiException(BankErrorCode.ACCOUNT_NOT_FOUND));
                });
        account.deductBalance(request.paymentBalance());
        accountRepository.save(account);

        // 가맹점의 친환경 여부(isGreen) 조회
        boolean isGreen = merchantRepository.findById(result.merchantId())
                .map(merchant -> Boolean.TRUE.equals(merchant.getIsGreen()))
                .orElse(false);

        // 결제 완료 이벤트 발행
        log.info("💳 카드 결제 성공, 이벤트 발행 준비 - uniqueNo: {}, merchant: {}, amount: {}", 
                result.transactionUniqueNo(), result.merchantName(), result.paymentBalance());
        LocalDate approvalDate = LocalDate.parse(result.transactionDate(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        eventPublisher.publishEvent(new CardPaymentEvent(
                userId,
                card.getWithdrawalAccountNo(),
                String.valueOf(result.transactionUniqueNo()),
                withdrawalUniqueNo, // 이체 시뮬레이션의 고유 번호 (중복 방지용)
                result.categoryName(),
                request.paymentBalance(),
                account.getBalance(), // 차감 후 잔액
                String.valueOf(result.merchantId()),
                result.merchantName(),
                approvalDate,
                result.transactionTime(),
                isGreen
        ));
    }

    /**
     * 카드 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public CardHistoryResponse getCardHistory(UUID userId, CardHistoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        Card card = cardRepository.findById(request.cardId())
                .orElseThrow(() -> new ApiException(BankErrorCode.CARD_NOT_FOUND));

        if (!card.getUserId().equals(userId)) {
            throw new ApiException(BankErrorCode.CARD_NOT_FOUND);
        }

        // 금융망에서 거래 내역 조회
        FinanceCardTransactionHistoryResponse.Rec rec = ssafyFinanceService.getCardTransactionHistory(
                user.getUserKey(),
                card.getCardNo(),
                card.getCvc(),
                request.startDate(),
                request.endDate()
        );

        List<CardHistoryResponse.CardTransactionDetails> list = rec.list().stream()
                .map(d -> new CardHistoryResponse.CardTransactionDetails(
                        d.transactionUniqueNo(),
                        d.categoryId(),
                        d.categoryName(),
                        d.merchantId(),
                        d.merchantName(),
                        d.transactionDate(),
                        d.transactionTime(),
                        d.transactionBalance()
                ))
                .collect(Collectors.toList());

        return new CardHistoryResponse(rec.estimatedBalance(), list);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization error", e);
            return null;
        }
    }
}
