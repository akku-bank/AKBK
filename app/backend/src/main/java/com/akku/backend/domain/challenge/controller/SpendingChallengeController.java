package com.akku.backend.domain.challenge.controller;

import com.akku.backend.domain.challenge.dto.SpendingChallengeDto;
import com.akku.backend.domain.challenge.service.SpendingChallengeService;
import com.akku.backend.global.dto.ApiResponse;
import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/challenges/spending")
@RequiredArgsConstructor
public class SpendingChallengeController {

    private final SpendingChallengeService spendingChallengeService;

    // 1. 소비 목표 계획 등록 (자녀)
    @PostMapping
    public ResponseEntity<ApiResponse<SpendingChallengeDto.CreateResponse>> createChallenge(
            @RequestBody SpendingChallengeDto.CreateRequest request,
            @RequestAttribute("userId") UUID userId) { // 시큐리티/인터셉터에서 넘겨준다고 가정

        // 서비스 단으로 넘겨서 비즈니스 로직 처리
        SpendingChallengeDto.CreateResponse response = spendingChallengeService.createChallenge(userId, request);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 제안이 등록되었습니다.", response));
    }

    // 2. 소비 목표 계획 수정 (자녀)
    @PatchMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.UpdateResponse>> updateChallenge(
            @PathVariable UUID challengeId,
            @RequestBody SpendingChallengeDto.UpdateRequest request,
            @RequestAttribute("userId") UUID userId) {

        SpendingChallengeDto.UpdateResponse response = spendingChallengeService.updateChallenge(userId, challengeId, request);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 제안이 수정되었습니다.", response));
    }

    // 3. 소비 목표 계획 삭제 (자녀)
    @DeleteMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(
            @PathVariable UUID challengeId,
            @RequestAttribute("userId") UUID userId) {

        spendingChallengeService.deleteChallenge(userId, challengeId);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 제안이 삭제되었습니다.", null));
    }

    // 4. 소비 목표 계획 승인/반려 (부모 전용)
    @PatchMapping("/{challengeId}/status")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.StatusUpdateResponse>> updateChallengeStatus(
            @PathVariable UUID challengeId,
            @RequestBody SpendingChallengeDto.StatusUpdateRequest request,
            @RequestAttribute("userId") UUID parentId) {

        SpendingChallengeDto.StatusUpdateResponse response = spendingChallengeService.updateChallengeStatus(parentId, challengeId, request);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 상태가 업데이트되었습니다.", response));
    }

    // 5. 차주 소비 목표 챌린지 목록 조회 (부모, 자녀 공통)
    @GetMapping
    public ResponseEntity<ApiResponse<SpendingChallengeDto.ListResponse>> getNextWeekChallenges(
            @RequestParam(required = false) UUID childId,
            @RequestParam(required = false) ChallengeStatus status,
            @RequestAttribute("userId") UUID requestUserId) {

        SpendingChallengeDto.ListResponse response = spendingChallengeService.getNextWeekChallenges(requestUserId, childId, status);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 목록을 불러왔습니다.", response));
    }

    // 6. 소비 목표 챌린지 단건 상세 조회 (부모, 자녀 공통)
    @GetMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.DetailResponse>> getChallengeDetail(
            @PathVariable UUID challengeId,
            @RequestAttribute("userId") UUID requestUserId) {

        SpendingChallengeDto.DetailResponse response = spendingChallengeService.getChallengeDetail(requestUserId, challengeId);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 상세 정보를 불러왔습니다.", response));
    }

    // 7. 미수령 보상 목록 조회 (자녀 전용) — 지난주 SUCCESS 챌린지만 반환
    @GetMapping("/unclaimed")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.UnclaimedListResponse>> getUnclaimedChallenges(
            @RequestAttribute("userId") UUID userId) {

        SpendingChallengeDto.UnclaimedListResponse response = spendingChallengeService.getUnclaimedChallenges(userId);

        return ResponseEntity.ok(ApiResponse.success("보상 요청 가능한 챌린지 목록을 불러왔습니다.", response));
    }

    // 8. 보상 요청 (자녀 전용) — SUCCESS + 지난주 챌린지에 대해서만 REWARD_REQUESTED로 전환
    @PostMapping("/{challengeId}/reward")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.RewardRequestResponse>> requestReward(
            @PathVariable UUID challengeId,
            @RequestAttribute("userId") UUID userId) {

        SpendingChallengeDto.RewardRequestResponse response = spendingChallengeService.requestReward(userId, challengeId);

        return ResponseEntity.ok(ApiResponse.success("보상 요청이 완료되었습니다. 부모님께 알림을 전송했습니다.", response));
    }
}