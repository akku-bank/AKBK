package com.akku.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 간편 로그인 요청 DTO
 */
public record LoginRequest(
        @NotBlank(message = "사용자 ID는 필수 입력 항목입니다.")
        String userId,

        @NotBlank(message = "PIN은 필수 입력 항목입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "PIN은 6자리 숫자여야 합니다.")
        String pin,

        String fcmToken // 로그인 시 기기 정보 갱신을 위해 추가
) {}
