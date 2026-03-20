package com.akku.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * PIN 변경 요청 DTO
 */
public record PinChangeRequest(
        @NotBlank(message = "기존 PIN은 필수 입력 항목입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "PIN은 6자리 숫자여야 합니다.")
        String oldPin,

        @NotBlank(message = "새로운 PIN은 필수 입력 항목입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "PIN은 6자리 숫자여야 합니다.")
        String newPin
) {}
