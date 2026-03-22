package com.akku.backend.domain.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "개별 친구 정보")
public class FriendDto {

    @Schema(description = "친구 유저 ID")
    private UUID friendId;

    @Schema(description = "친구 이름")
    private String name;

    @Schema(description = "친구 아바타 정보")
    private String avatarImage;

    @Builder
    public FriendDto(UUID friendId, String name, String avatarImage) {
        this.friendId = friendId;
        this.name = name;
        this.avatarImage = avatarImage;
    }
}
