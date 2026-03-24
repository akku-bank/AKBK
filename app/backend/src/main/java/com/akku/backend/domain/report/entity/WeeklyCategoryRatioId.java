package com.akku.backend.domain.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class WeeklyCategoryRatioId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "start_day")
    private LocalDate startDay;

    @Column(name = "sub_category_id")
    private Integer subCategoryId;
}
