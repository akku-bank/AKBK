package com.akku.backend.domain.report.entity;

import com.akku.backend.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "weekly_category_ratio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WeeklyCategoryRatio {

    @EmbeddedId
    private WeeklyCategoryRatioId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "sub_category_name")
    private String subCategoryName;

    private BigDecimal ratio;

    @Column(name = "spending_amount", nullable = false)
    private Long spendingAmount;
}
