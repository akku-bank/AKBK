package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "카드 거래 내역 조회 요청")
public record CardHistoryRequest(
    @Schema(description = "카드 ID")
    @NotNull UUID cardId,
    
    @Schema(description = "조회 시작일 (YYYYMMDD)")
    @NotBlank String startDate,
    
    @Schema(description = "조회 종료일 (YYYYMMDD)")
    @NotBlank String endDate
) {}
