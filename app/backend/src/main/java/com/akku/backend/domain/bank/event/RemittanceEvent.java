package com.akku.backend.domain.bank.event;

import java.util.UUID;

/**
 * 계좌 이체(송금) 완료 이벤트.
 * AccountService에서 발행하며, 알림 및 기타 도메인에서 구독한다.
 */
public record RemittanceEvent(
    UUID userId,
    String departureAccountNumber,
    String targetBankCode,
    String targetAccountNumber,
    String targetName,
    Long amount,
    Long balanceAfter,
    String transactionUniqueNo
) {}
