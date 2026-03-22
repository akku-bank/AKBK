package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래 내역 노출 여부 변경 요청")
public record TransactionVisibilityRequest(
    @Schema(description = "숨김 여부 (true: 숨김, false: 노출)", example = "true")
    Boolean isHidden
) {
}
