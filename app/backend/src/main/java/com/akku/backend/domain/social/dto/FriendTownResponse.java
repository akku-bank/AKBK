package com.akku.backend.domain.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "친구 타운 방문 응답")
public record FriendTownResponse(
        @Schema(description = "친구 이름")
        String friendName,

        @Schema(description = "친구 아바타 정보")
        AvatarDto avatar,

        @Schema(description = "최근 기부처 이름")
        String recentCharity
) {
    public record AvatarDto(
            @Schema(description = "눈 모양")
            String eyeType,

            @Schema(description = "피부 색상")
            String skinColor,

            @Schema(description = "장착된 아이템 리소스 URL 목록")
            List<String> equippedItems
    ) {}
}
