package com.akku.backend.domain.bank.exception;

import com.akku.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BankErrorCode implements ErrorCode {
    ACCOUNT_NOT_FOUND("BANK_001", HttpStatus.NOT_FOUND, "계좌 정보를 찾을 수 없습니다"),
    INSUFFICIENT_BALANCE("BANK_002", HttpStatus.BAD_REQUEST, "잔액이 부족합니다"),
    TRANSFER_LIMIT_EXCEEDED("BANK_003", HttpStatus.BAD_REQUEST, "이체 한도를 초과했습니다"),
    INVALID_TRANSACTION_REQUEST("BANK_004", HttpStatus.BAD_REQUEST, "잘못된 결제/이체 요청입니다"),
    ALREADY_EXISTS_ACCOUNT("BANK_005", HttpStatus.CONFLICT, "이미 등록된 계좌입니다"),
    CARD_PRODUCT_NOT_FOUND("BANK_006", HttpStatus.NOT_FOUND, "카드 상품 정보를 찾을 수 없습니다"),
    CARD_NOT_FOUND("BANK_007", HttpStatus.NOT_FOUND, "카드 정보를 찾을 수 없습니다"),
    INVALID_PAYMENT_TOKEN("BANK_008", HttpStatus.UNAUTHORIZED, "유효하지 않거나 이미 사용된 결제 토큰입니다"),
    EXPIRED_PAYMENT_TOKEN("BANK_009", HttpStatus.GONE, "만료된 결제 토큰입니다");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}
