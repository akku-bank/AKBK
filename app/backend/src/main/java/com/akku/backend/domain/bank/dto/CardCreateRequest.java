package com.akku.backend.domain.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CardCreateRequest(
    @NotNull(message = "카드 상품 ID는 필수입니다.")
    UUID cardProductId,

    @NotBlank(message = "출금 계좌 번호는 필수입니다.")
    String withdrawalAccountNo,

    @NotBlank(message = "결제일(출금일)은 필수입니다.")
    String withdrawalDate
) {}
