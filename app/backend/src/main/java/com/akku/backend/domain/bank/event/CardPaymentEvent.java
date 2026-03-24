package com.akku.backend.domain.bank.event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 카드 결제 완료 이벤트.
 * bank 도메인(CardService)에서 발행하고, challenge 도메인(CardPaymentEventListener)에서 구독한다.
 * 단방향 의존(challenge → bank)이므로 도메인 결합이 발생하지 않는다.
 */
public record CardPaymentEvent(
        UUID userId,
        String subCategoryName,
        Long paymentBalance,
        LocalDate transactionDate
) {}
