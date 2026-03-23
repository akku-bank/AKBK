package com.akku.backend.domain.social.controller;

import com.akku.backend.domain.social.dto.FriendInviteData;
import com.akku.backend.domain.social.dto.FriendInformationData;
import com.akku.backend.domain.social.service.FriendService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Social", description = "소셜/친구 관련 API")
@RestController
@RequestMapping("/api/social/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 친구 초대 링크 생성
     */
    @Operation(summary = "친구 초대 링크 생성", description = "본인의 고유 초대 코드를 생성하거나 기존 코드를 조회합니다.")
    @PostMapping("/invites")
    @PreAuthorize("hasRole('CHILD')")
    public ResponseEntity<ApiResponse<FriendInviteData>> createInviteCode(
            @AuthenticationPrincipal UUID userId
    ) {
        FriendInviteData data = friendService.createInviteCode(userId);
        return ResponseEntity.ok(ApiResponse.success("초대 링크가 생성되었습니다.", data));
    }

    /**
     * 초대 코드 정보 조회
     * 초대받은 사람이 초대자의 정보를 확인하기 위해 사용
     */
    @Operation(summary = "초대 코드 정보 조회", description = "초대 코드에 해당하는 초대자 정보를 조회합니다.")
    @GetMapping("/invites/{inviteCode}")
    @PreAuthorize("hasRole('CHILD')")
    public ResponseEntity<ApiResponse<FriendInformationData>> getInviteInfo(
            @PathVariable String inviteCode
    ) {
        FriendInformationData data = friendService.getInviteInfo(inviteCode);
        return ResponseEntity.ok(ApiResponse.success("초대자 정보를 불러왔습니다.", data));
    }
}
