package com.akku.backend.domain.bank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CardProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "card_unique_no", unique = true, nullable = false, length = 20)
    private String cardUniqueNo;

    @Column(name = "card_issuer_code", length = 10)
    private String cardIssuerCode;

    @Column(name = "card_issuer_name", length = 100)
    private String cardIssuerName;

    @Column(name = "card_name", nullable = false, length = 255)
    private String cardName;

    @Column(name = "card_description", length = 1000)
    private String cardDescription;

    @Column(name = "card_type_code", length = 10)
    private String cardTypeCode;

    @Column(name = "card_type_name", length = 50)
    private String cardTypeName;

    @Column(name = "base_limit_performance")
    private Long baseLimitPerformance;

    @Column(name = "max_benefit_limit")
    private Long maxBenefitLimit;

    @Column(name = "card_benefit_info", length = 2000)
    private String cardBenefitInfo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String cardName, String cardDescription, Long baseLimitPerformance, Long maxBenefitLimit, String cardBenefitInfo) {
        this.cardName = cardName;
        this.cardDescription = cardDescription;
        this.baseLimitPerformance = baseLimitPerformance;
        this.maxBenefitLimit = maxBenefitLimit;
        this.cardBenefitInfo = cardBenefitInfo;
    }
}
