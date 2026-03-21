package com.akku.backend.domain.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "친구 초대 데이터")
public record FriendInviteData(
        @Schema(description = "초대 코드")
        String inviteCode
) {}
