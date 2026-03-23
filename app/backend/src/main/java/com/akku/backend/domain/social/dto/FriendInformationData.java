package com.akku.backend.domain.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "친구 정보 (초대자)")
public record FriendInformationData(
        @Schema(description = "초대한 유저 ID")
        UUID inviterId,
        @Schema(description = "초대한 유저 이름")
        String inviterName,
        @Schema(description = "초대한 유저 아바타 이미지 URL 목록")
        List<String> inviterAvatarUrls,
        @Schema(description = "코드 유효 여부")
        boolean isValid
) {}
