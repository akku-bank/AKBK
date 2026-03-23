package com.akku.backend.domain.bank.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이체(transfer) 토픽에서 수신하는 이벤트 페이로드.
 *
 * <p>eventId 를 DB 저장 시 PK로 사용하여 중복 처리를 방지한다.</p>
 */
public record TransferEvent(

        @JsonProperty("event_id")
        UUID eventId,

        @JsonProperty("transaction_id")
        String transactionId,

        @JsonProperty("user_id")
        UUID userId,

        @JsonProperty("account_id")
        UUID accountId,

        Long amount,

        /** SPEND | INCOME */
        @JsonProperty("transaction_type")
        String transactionType,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("sender_account_id")
        UUID senderAccountId,

        @JsonProperty("receiver_account_id")
        UUID receiverAccountId,

        @JsonProperty("sub_category_id")
        Integer subCategoryId,

        @JsonProperty("sub_category_name")
        String subCategoryName
) {}
