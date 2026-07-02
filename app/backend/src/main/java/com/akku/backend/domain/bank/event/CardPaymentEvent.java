package com.akku.backend.domain.bank.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 카드 결제 완료 이벤트.
 * bank 도메인(CardService)에서 발행하고, challenge 도메인 리스너들이 구독한다.
 * 단방향 의존(challenge → bank)이므로 도메인 결합이 발생하지 않는다.
 *
 * isGreen: 해당 결제의 가맹점이 녹색 가맹점(merchant.is_green = true)인지 여부.
 *          CardService가 결제 처리 시 merchant 테이블을 조회하여 세팅한다.
 *          SpendingChallengeCardPaymentEventListener는 이 필드를 무시한다.
 */
public record CardPaymentEvent(
    UUID userId,
    String withdrawalAccountNo,
    @JsonProperty("transactionUniqueNo") String transactionUniqueNo,
    String withdrawalUniqueNo,
    String subCategoryName,
    Long paymentBalance,
    Long balanceAfter,
    String merchantId,
    String merchantName,
    LocalDate transactionDate,
    String transactionTime,
    boolean isGreen
) {}
