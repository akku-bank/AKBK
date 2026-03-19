package com.akku.backend.domain.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record CardCreateRequest(
    @NotNull(message = "카드 상품 ID는 필수입니다.")
    UUID cardProductId,

    @NotBlank(message = "출금 계좌 번호는 필수입니다.")
    String withdrawalAccountNo,

    @NotBlank(message = "결제일(출금일)은 필수입니다.")
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])$", message = "결제일은 01부터 31 사이의 2자리 숫자여야 합니다.")
    String withdrawalDate
) {}
