package com.akku.backend.domain.bank.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * 실시간 거래 내역 이벤트 (Kafka 발행용)
 */
public record TransactionEvent(
        @JsonProperty("event_type")
        String eventType,
        
        Data data
) implements Serializable {
    
    public record Data(
            String id,                          // Transaction.id (UUID)
            
            @JsonProperty("user_id")
            String userId,                      // User.id (UUID 문자열)
            
            @JsonProperty("account_id")
            String accountId,                   // Account.id (UUID)
            
            @JsonProperty("merchant_id")
            Long merchantId,                    // Transaction.merchantId
            
            Long amount,                        // 거래 금액
            
            @JsonProperty("transaction_type")
            String transactionType,             // "INCOME", "SPEND", "TRANSFER"
            
            @JsonProperty("sub_category_id")
            Integer subCategoryId,              // 지출 카테고리 ID (Transaction.subCategoryId)
            
            @JsonProperty("sub_category_name")
            String subCategoryName,             // Transaction.subCategoryName
            
            @JsonProperty("merchant_name")
            String merchantName,                // Transaction.merchantName
            
            @JsonProperty("create_at")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            String createdAt                    // ISO 8601 형식
    ) implements Serializable {}
}
