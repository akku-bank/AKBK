package com.akku.backend.domain.bank.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * transaction 토픽에 발행하는 통합 거래 완료 이벤트.
 *
 * <h3>Spark 규격 준수</h3>
 * <ul>
 *   <li>{@code event_type}: "TRANSACTION_COMPLETED" 고정</li>
 *   <li>{@code data.created_at} / {@code data.saved_at}: "yyyy-MM-dd HH:mm:ss" 포맷</li>
 *   <li>Spark 필수 필드 구조 변경 금지, 후처리 필드는 추가만 허용</li>
 * </ul>
 *
 * <h3>토픽 컨슈머</h3>
 * <ul>
 *   <li>TransactionNotificationConsumer: 실시간 FCM 알림</li>
 *   <li>TransactionJellyConsumer: 젤링 계산 (PAYMENT + is_jelly_target=true 한정)</li>
 *   <li>Spark: 리포트 연산 (Spring 외부에서 직접 소비 — Spring에서는 구현하지 않음)</li>
 * </ul>
 */
public record TransactionCompletedEvent(

        @JsonProperty("event_type")
        String eventType,

        Data data
) {
    private static final DateTimeFormatter SPARK_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public record Data(

            /** DB에 저장된 transactions.id (DB 생성 UUID). Kafka eventId와 별개. Spark 고유 식별자. */
            String id,

            @JsonProperty("user_id")
            String userId,

            Long amount,

            /** SPEND | INCOME */
            @JsonProperty("transaction_type")
            String transactionType,

            @JsonProperty("sub_category_id")
            Integer subCategoryId,

            @JsonProperty("sub_category_name")
            String subCategoryName,

            /** yyyy-MM-dd HH:mm:ss */
            @JsonProperty("created_at")
            String createdAt,

            /** PAYMENT | TRANSFER | DEPOSIT */
            @JsonProperty("event_source")
            String eventSource,

            @JsonProperty("account_id")
            String accountId,

            /** null 허용 (TRANSFER/DEPOSIT 은 false 로 세팅) */
            @JsonProperty("is_jelly_target")
            Boolean isJellyTarget,

            /** DB 저장 완료 시각 (yyyy-MM-dd HH:mm:ss) */
            @JsonProperty("saved_at")
            String savedAt
    ) {}

    // ── 팩토리 메서드 ──────────────────────────────────────────────────────────
    // id: DB가 생성한 Transaction.id (Spark 고유 식별자로 사용)

    public static TransactionCompletedEvent fromPayment(
            com.akku.backend.domain.bank.entity.Transaction saved,
            PaymentEvent e,
            LocalDateTime savedAt) {
        return new TransactionCompletedEvent(
                "TRANSACTION_COMPLETED",
                new Data(
                        saved.getId().toString(),
                        e.userId().toString(),
                        e.amount(),
                        e.transactionType(),
                        e.subCategoryId(),
                        e.subCategoryName(),
                        format(e.createdAt()),
                        "PAYMENT",
                        e.accountId().toString(),
                        e.isJellyTarget(),
                        format(savedAt)
                )
        );
    }

    public static TransactionCompletedEvent fromTransfer(
            com.akku.backend.domain.bank.entity.Transaction saved,
            TransferEvent e,
            LocalDateTime savedAt) {
        return new TransactionCompletedEvent(
                "TRANSACTION_COMPLETED",
                new Data(
                        saved.getId().toString(),
                        e.userId().toString(),
                        e.amount(),
                        e.transactionType(),
                        e.subCategoryId(),
                        e.subCategoryName(),
                        format(e.createdAt()),
                        "TRANSFER",
                        e.accountId().toString(),
                        false,
                        format(savedAt)
                )
        );
    }

    public static TransactionCompletedEvent fromDeposit(
            com.akku.backend.domain.bank.entity.Transaction saved,
            DepositEvent e,
            LocalDateTime savedAt) {
        return new TransactionCompletedEvent(
                "TRANSACTION_COMPLETED",
                new Data(
                        saved.getId().toString(),
                        e.userId().toString(),
                        e.amount(),
                        e.transactionType(),
                        e.subCategoryId(),
                        e.subCategoryName(),
                        format(e.createdAt()),
                        "DEPOSIT",
                        e.accountId().toString(),
                        false,
                        format(savedAt)
                )
        );
    }

    private static String format(LocalDateTime dt) {
        return dt == null ? null : dt.format(SPARK_FMT);
    }
}
