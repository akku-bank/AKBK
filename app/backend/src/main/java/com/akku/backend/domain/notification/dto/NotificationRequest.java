package com.akku.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알림 전송 요청 DTO")
public class NotificationRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    @Schema(description = "대상 기기의 FCM 토큰", example = "fcm_token_example")
    private String token;

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "알림 제목", example = "아꾸뱅꾸 알림")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(description = "알림 내용", example = "새로운 거래 내역이 있습니다.")
    private String body;

    @Schema(description = "이미지 URL (선택)", example = "https://example.com/image.png")
    private String imageUrl;
}
