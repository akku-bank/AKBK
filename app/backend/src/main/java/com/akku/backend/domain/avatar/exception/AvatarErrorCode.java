package com.akku.backend.domain.avatar.exception;

import com.akku.backend.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 아바타 도메인 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum AvatarErrorCode implements ErrorCode {

    // --- [1. 상점 및 조회 관련] ---
    ITEM_NOT_FOUND("AVATAR_001", HttpStatus.NOT_FOUND, "해당 아이템을 찾을 수 없습니다."),

    // --- [2. 구매 관련] ---
    ALREADY_OWNED_ITEM("AVATAR_002", HttpStatus.CONFLICT, "이미 보유하고 있는 아이템입니다."),
    INSUFFICIENT_JELLINGS("AVATAR_003", HttpStatus.BAD_REQUEST, "젤링 잔액이 부족하여 구매할 수 없습니다."),
    LEVEL_TOO_LOW("AVATAR_004", HttpStatus.FORBIDDEN, "아이템 구매를 위한 소비 레벨이 부족합니다."),

    // --- [3. 장착 및 인벤토리 관련] ---
    ITEM_NOT_OWNED("AVATAR_005", HttpStatus.FORBIDDEN, "보유하지 않은 아이템은 장착하거나 해제할 수 없습니다."),
    INVALID_ITEM_CATEGORY("AVATAR_006", HttpStatus.BAD_REQUEST, "잘못된 아이템 카테고리입니다."),

    // --- [4. 외형 커스텀 관련 ---
    INSUFFICIENT_CUSTOM_TICKETS("AVATAR_007", HttpStatus.BAD_REQUEST, "외형 변경을 위한 커스텀 티켓이 부족합니다.");


    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

}
