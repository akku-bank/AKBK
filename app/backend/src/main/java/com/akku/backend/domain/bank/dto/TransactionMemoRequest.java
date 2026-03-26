package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "거래 내역 메모 수정 요청")
public record TransactionMemoRequest(
    @Size(max = 255, message = "메모는 255자 이내여야 합니다.")
    @Schema(description = "수정할 메모 내용", example = "친구랑 점심 식사")
    String memo
) {}
