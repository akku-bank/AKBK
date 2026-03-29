package com.akku.backend.domain.bank.event;

import java.util.UUID;

/**
 * 계좌 입금(수신) 완료 이벤트.
 * 내부 사용자 간 이체 시 수취인에게 알림을 보내기 위해 사용한다.
 */
public record DepositReceivedEvent(
    UUID userId,               // 수취인 ID
    String senderName,         // 송금인 이름 (또는 별칭)
    Long amount,               // 입금액
    Long balanceAfter,         // 입금 후 잔액
    String transactionUniqueNo // 거래 고유 번호
) {}
