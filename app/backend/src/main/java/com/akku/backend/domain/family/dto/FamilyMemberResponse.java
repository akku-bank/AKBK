package com.akku.backend.domain.family.dto;

import java.util.UUID;

/**
 * 가족 구성원 개별 정보 DTO
 */
public record FamilyMemberResponse(
        UUID profileId,      // 추가: 미연동 상태일 때 구성원을 식별/삭제하기 위한 프로필 고유 ID
        UUID userId,         // 연동 완료 시 존재, 미연동 시 null
        String name,         // 구성원 이름
        String role,         // PARENT or CHILD
        UUID accountId       // 연동 완료 시 존재, 미연동 시 null (송금용)
) {
}