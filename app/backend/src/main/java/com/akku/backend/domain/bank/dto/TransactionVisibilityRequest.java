package com.akku.backend.domain.bank.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래 내역 노출 여부 변경 요청")
public record TransactionVisibilityRequest(
    @NotNull(message = "숨김 여부는 필수입니다.")
    @Schema(description = "숨김 여부 (true: 숨김, false: 노출)", example = "true")
    Boolean isHidden
) {
}
