package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래 내역 전체 노출 여부 변경 응답 데이터")
public record TransactionVisibilityResponse(
    @Schema(description = "최종 반영된 숨김 상태", example = "true")
    Boolean isHidden
) {
}
