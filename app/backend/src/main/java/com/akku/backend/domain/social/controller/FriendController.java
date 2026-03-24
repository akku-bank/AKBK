package com.akku.backend.domain.social.controller;

import com.akku.backend.domain.social.dto.FriendInviteData;
import com.akku.backend.domain.social.dto.FriendInformationData;
import com.akku.backend.domain.social.dto.FriendListResponse;
import com.akku.backend.domain.social.dto.FriendTownResponse;
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
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 친구 초대 링크 생성
     */
    @Operation(summary = "친구 초대 링크 생성", description = "본인의 고유 초대 코드를 생성하거나 기존 코드를 조회합니다.")
    @PostMapping("/friends/invites")
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
    @GetMapping("/friends/invites/{inviteCode}")
    @PreAuthorize("hasRole('CHILD')")
    public ResponseEntity<ApiResponse<FriendInformationData>> getInviteInfo(
            @PathVariable String inviteCode
    ) {
        FriendInformationData data = friendService.getInviteInfo(inviteCode);
        return ResponseEntity.ok(ApiResponse.success("초대자 정보를 불러왔습니다.", data));
    }

    /**
     * 친구 코드 입력으로 친구 맺기
     */
    @Operation(summary = "친구 코드로 친구 맺기", description = "친구 초대 코드를 입력하여 친구를 맺습니다.")
    @PostMapping("/friends/invites/{inviteCode}/accept")
    @PreAuthorize("hasRole('CHILD')")
    public ResponseEntity<ApiResponse<Void>> acceptFriendInvite(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String inviteCode
    ) {
        friendService.acceptFriendInvite(userId, inviteCode);
        return ResponseEntity.ok(ApiResponse.success("친구가 되었습니다."));
    }

    /**
     * 친구 목록 조회
     */
    @Operation(summary = "친구 목록 조회", description = "나의 친구 목록을 조회합니다.")
    @GetMapping("/friends")
    @PreAuthorize("hasRole('CHILD')")
    public ResponseEntity<ApiResponse<FriendListResponse>> getFriends(
            @AuthenticationPrincipal UUID userId
    ) {
        FriendListResponse data = friendService.getFriendList(userId);
        return ResponseEntity.ok(ApiResponse.success("친구 목록을 조회했습니다.", data));
    }

    /**
     * 친구 삭제
     */
    @Operation(summary = "친구 삭제", description = "특정 친구를 삭제합니다.")
    @DeleteMapping("/friends/{friendId}")
    @PreAuthorize("hasRole('CHILD')")
    public ResponseEntity<ApiResponse<Void>> deleteFriend(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID friendId
    ) {
        friendService.deleteFriend(userId, friendId);
        return ResponseEntity.ok(ApiResponse.success("친구를 삭제했습니다."));
    }

    /**
     * 친구 타운 정보 조회
     */
    @Operation(summary = "친구 타운 정보 조회", description = "친구의 타운 방문 시 필요한 정보를 조회합니다.")
    @GetMapping("/town/{friendId}")
    @PreAuthorize("hasRole('CHILD')")
    public ResponseEntity<ApiResponse<FriendTownResponse>> getFriendTown(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID friendId
    ) {
        FriendTownResponse data = friendService.getFriendTown(userId, friendId);
        return ResponseEntity.ok(ApiResponse.success("친구 타운 정보를 조회했습니다.", data));
    }
}
