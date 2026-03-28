package com.akku.backend.domain.bank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name = "sub_category_id")
    private Integer subCategoryId;

    @Column(name = "sub_category_name", length = 50)
    private String subCategoryName;

    @Column(name = "transaction_unique_no", nullable = false, unique = true)
    private String transactionUniqueNo;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "balance_after")
    private Long balanceAfter;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType; // 1: 입금, 2: 출금

    @Column(name = "merchant_name", length = 255)
    private String merchantName;

    public String getPlace() {
        if (this.merchantName != null && !this.merchantName.trim().isEmpty()) {
            return this.merchantName;
        }
        if (this.memo != null && !this.memo.trim().isEmpty()) {
            return this.memo;
        }
        return "1".equals(this.transactionType) ? "입금 내역" : "결제 내역";
    }

    @Column(name = "transaction_date", length = 14)
    private String date;

    @Builder.Default
    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @Column(name = "memo", length = 255)
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void updateVisibility(boolean isHidden) {
        this.isHidden = isHidden;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updateMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public void updateSubCategoryName(String subCategoryName) {
        this.subCategoryName = subCategoryName;
    }
}
