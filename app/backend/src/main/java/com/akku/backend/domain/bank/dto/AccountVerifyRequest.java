package com.akku.backend.domain.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountVerifyRequest(
    @NotBlank(message = "은행 코드는 필수입니다.")
    @Pattern(regexp = "\\d{3}", message = "은행 코드는 숫자 3자리여야 합니다.")
    String bankCode,

    @NotBlank(message = "계좌번호는 필수입니다.")
    @Pattern(regexp = "^[0-9]+$", message = "계좌번호는 숫자만 입력 가능합니다.")
    String accountNumber
) {}
