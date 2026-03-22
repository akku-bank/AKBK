package com.akku.backend.domain.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "친구 목록 조회 응답")
public class FriendListResponse {

    @Schema(description = "친구 목록 배열")
    private List<FriendDto> friends;
}
