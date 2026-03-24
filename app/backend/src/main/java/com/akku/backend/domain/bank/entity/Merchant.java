package com.akku.backend.domain.bank.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "merchant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "is_green")
    private Boolean isGreen = false;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "sub_category_id")
    private Integer subCategoryId;

    @Column(name = "sub_category_name")
    private String subCategoryName;
}
