package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "1원 송금 인증 요청 응답")
public record AccountVerifyResponse(
    @Schema(description = "인증 코드 (4자리)", example = "4829")
    String authCode
) {}
