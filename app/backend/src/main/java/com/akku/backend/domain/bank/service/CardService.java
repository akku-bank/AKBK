package com.akku.backend.domain.bank.service;

import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.entity.Card;
import com.akku.backend.domain.bank.entity.CardProduct;
import com.akku.backend.domain.bank.event.CardPaymentEvent;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.auth.service.SsafyFinanceService;
import com.akku.backend.domain.bank.exception.BankErrorCode;
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
    private final SsafyFinanceService ssafyFinanceService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

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
                    .orElseGet(() -> {
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
                                .build();
                        return cardProductRepository.save(newProduct);
                    });
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
            // 해당 카드 번호가 이미 있는지 확인
            boolean exists = cardRepository.findAllByUserId(userId).stream()
                    .anyMatch(card -> card.getCardNo().equals(c.cardNo()));

            if (!exists) {
                // 카드 상품 정보 조회
                CardProduct product = cardProductRepository.findByCardUniqueNo(c.cardUniqueNo())
                        .orElseGet(() -> {
                            return cardProductRepository.save(CardProduct.builder()
                                    .cardUniqueNo(c.cardUniqueNo())
                                    .cardIssuerCode(c.cardIssuerCode())
                                    .cardIssuerName(c.cardIssuerName())
                                    .cardName(c.cardName())
                                    .baseLimitPerformance(c.baseLimitPerformance())
                                    .maxBenefitLimit(c.maxBenefitLimit())
                                    .cardDescription(c.cardDescription())
                                    .build());
                        });

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

        // 결제 완료 이벤트 발행 (트랜잭션 커밋 후 실행되도록 분리)
        // 외부 결제가 성공했으므로, 이후의 부가 로직(이벤트 발행 등) 실패가 결제를 롤백시키지 않도록 함
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    LocalDate approvalDate = LocalDate.parse(result.transactionDate(), DateTimeFormatter.ofPattern("yyyyMMdd"));
                    
                    // 가맹점의 친환경 여부(isGreen) 조회
                    boolean isGreen = merchantRepository.findById(result.merchantId())
                            .map(merchant -> Boolean.TRUE.equals(merchant.getIsGreen()))
                            .orElse(false);

                    eventPublisher.publishEvent(new CardPaymentEvent(
                            userId,
                            result.categoryName(),
                            result.paymentBalance(),
                            approvalDate,
                            isGreen
                    ));
                } catch (Exception e) {
                    // 이벤트 발행 중 오류가 발생해도 로그만 남기고 무시 (결제는 유지)
                    log.error("카드 결제 완료 후 이벤트 발행 실패. userId: {}, result: {}", userId, result, e);
                }
            }
        });
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
}
