package com.akku.backend.domain.bank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 거래 내역 엔티티. {@code transactions} 테이블에 매핑된다.
 *
 * <h3>Kafka 파이프라인 연동</h3>
 * <p>{@code event_id} 컬럼은 Kafka 이벤트의 eventId 를 저장하여 중복 INSERT 를 방지한다.
 * Finance API 등 Kafka 외 경로로 저장된 레코드는 {@code event_id} 가 null 이다.</p>
 *
 * <h3>merchant_name 재사용 정책</h3>
 * <p>{@code merchant_name} 컬럼은 결제(PAYMENT) 시 가맹점명,
 * 입금(DEPOSIT) 시 입금자명을 저장하는 용도로 재사용된다.
 * 이체(TRANSFER) 는 해당 값을 저장하지 않는다.</p>
 *
 * <h3>sub_category_name</h3>
 * <p>거래 소분류명. ERD 기준 컬럼명은 {@code sub_category_name} 이다.</p>
 */
@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    @Column(name = "sub_category_name", length = 50)
    private String subCategoryName;

    /** 결제 시 가맹점명, 입금 시 입금자명으로 재사용. 이체 시 null. */
    @Column(name = "merchant_name", length = 100)
    private String merchantName;

    /** Kafka 이벤트 중복 방지용. Kafka 외 경로(Finance API 등) 저장 시 null. */
    @Column(name = "event_id", unique = true)
    private UUID eventId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── 정적 팩토리 ───────────────────────────────────────────────────────────

    public static Transaction fromPayment(com.akku.backend.domain.bank.event.PaymentEvent e) {
        return Transaction.builder()
                .accountId(e.accountId())
                .merchantId(e.merchantId())
                .amount(e.amount())
                .transactionType(e.transactionType())
                .subCategoryName(e.subCategoryName())
                .merchantName(e.merchantName())
                .eventId(e.eventId())
                .build();
    }

    public static Transaction fromTransfer(com.akku.backend.domain.bank.event.TransferEvent e) {
        return Transaction.builder()
                .accountId(e.accountId())
                .amount(e.amount())
                .transactionType(e.transactionType())
                .subCategoryName(e.subCategoryName())
                .eventId(e.eventId())
                .build();
    }

    public static Transaction fromDeposit(com.akku.backend.domain.bank.event.DepositEvent e) {
        return Transaction.builder()
                .accountId(e.accountId())
                .amount(e.amount())
                .transactionType(e.transactionType())
                .subCategoryName(e.subCategoryName())
                .merchantName(e.depositorName())  // 입금자명을 merchant_name 컬럼에 저장
                .eventId(e.eventId())
                .build();
    }
}
