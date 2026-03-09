package com.akku.backend.domain.family.dto;

import java.time.LocalDate;

/**
 * 가족 구성원 정보 부분 수정 요청 DTO
 * 값이 null로 들어오면 해당 필드는 수정 X
 */
public record FamilyMemberUpdateRequest(
        String name,
        LocalDate birthDate
) {
}