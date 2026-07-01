package com.akku.backend.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "계좌 실주명 조회 응답")
public record AccountHolderResponse(
    @Schema(description = "계좌 일련번호", example = "1234567890")
    String accountNumber,
    
    @Schema(description = "은행 코드", example = "001")
    String bankCode,
    
    @Schema(description = "예금주 성명", example = "김싸피")
    String userName
) {}
