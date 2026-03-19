package com.akku.backend.domain.bank.entity;

import com.akku.backend.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_product_id", nullable = false)
    private UUID cardProductId;

    @Column(name = "card_no", unique = true, nullable = false, length = 20)
    private String cardNo;

    @Column(name = "cvc", nullable = false, length = 3)
    private String cvc;

    @Column(name = "card_expiry_date", nullable = false, length = 8)
    private String cardExpiryDate;

    @Column(name = "withdrawal_account_no", length = 20)
    private String withdrawalAccountNo;

    @Column(name = "withdrawal_date", length = 2)
    private String withdrawalDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_product_id", insertable = false, updatable = false)
    private CardProduct cardProduct;
}
