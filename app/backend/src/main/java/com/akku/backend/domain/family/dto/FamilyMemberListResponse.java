package com.akku.backend.domain.family.dto;

import java.util.List;

/**
 * 가족 구성원 목록 응답 래퍼 DTO (data.members 구조 맞춤)
 */
public record FamilyMemberListResponse(
        List<FamilyMemberResponse> members
) {
}