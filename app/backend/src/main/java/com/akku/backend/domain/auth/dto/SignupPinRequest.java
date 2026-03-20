package com.akku.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * PIN 초기 설정 요청 DTO
 */
public record SignupPinRequest(
        @NotBlank(message = "PIN은 필수 입력 항목입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "PIN은 6자리 숫자여야 합니다.")
        String pin
) {}
