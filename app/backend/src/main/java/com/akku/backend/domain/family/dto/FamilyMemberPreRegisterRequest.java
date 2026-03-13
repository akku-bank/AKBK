package com.akku.backend.domain.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 가족 구성원 사전 등록(빈 의자 생성) 요청 DTO
 */
public record FamilyMemberPreRegisterRequest(
        @NotBlank(message = "구성원 이름은 필수입니다.")
        String name,

        @NotBlank(message = "역할(CHILD/PARENT)은 필수입니다.")
        String role,

        @NotNull(message = "생년월일은 필수입니다.")
        LocalDate birthDate
) {
}