package com.akku.backend.domain.challenge.controller;

import com.akku.backend.domain.challenge.dto.SpendingChallengeDto;
import com.akku.backend.domain.challenge.service.SpendingChallengeService;
import com.akku.backend.global.dto.ApiResponse;
import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Spending Challenge API", description = "소비 목표 챌린지 등록, 조회, 수정, 삭제 및 보상 처리")
@RestController
@RequestMapping("/api/challenges/spending")
@RequiredArgsConstructor
public class SpendingChallengeController {

    private final SpendingChallengeService spendingChallengeService;

    /**
     * 1. 소비 목표 챌린지 등록 (자녀 전용)
     */
    @Operation(
            summary = "소비 목표 챌린지 등록",
            description = "자녀가 다음 주 소비 목표 챌린지를 제안합니다. 등록된 챌린지는 PENDING 상태로 부모의 승인을 기다립니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "챌린지 등록 성공")
    @PostMapping
    public ResponseEntity<ApiResponse<SpendingChallengeDto.CreateResponse>> createChallenge(
            @Valid @RequestBody SpendingChallengeDto.CreateRequest request,
            @AuthenticationPrincipal UUID userId) {

        SpendingChallengeDto.CreateResponse response = spendingChallengeService.createChallenge(userId, request);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 제안이 등록되었습니다.", response));
    }

    /**
     * 2. 소비 목표 챌린지 수정 (자녀 전용)
     */
    @Operation(
            summary = "소비 목표 챌린지 수정",
            description = "자녀가 PENDING 또는 REJECTED 상태인 챌린지의 카테고리, 목표 소비액, 보상 금액을 수정합니다. 챌린지 시작일(월요일) 이후에는 수정이 불가합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "챌린지 수정 성공")
    @PatchMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.UpdateResponse>> updateChallenge(
            @Parameter(description = "수정할 챌린지 ID") @PathVariable UUID challengeId,
            @RequestBody SpendingChallengeDto.UpdateRequest request,
            @AuthenticationPrincipal UUID userId) {

        SpendingChallengeDto.UpdateResponse response = spendingChallengeService.updateChallenge(userId, challengeId, request);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 제안이 수정되었습니다.", response));
    }

    /**
     * 3. 소비 목표 챌린지 삭제 (자녀 전용)
     */
    @Operation(
            summary = "소비 목표 챌린지 삭제",
            description = "자녀가 PENDING 또는 REJECTED 상태인 챌린지를 삭제합니다. 이미 승인된 챌린지는 삭제할 수 없습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "챌린지 삭제 성공")
    @DeleteMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(
            @Parameter(description = "삭제할 챌린지 ID") @PathVariable UUID challengeId,
            @AuthenticationPrincipal UUID userId) {

        spendingChallengeService.deleteChallenge(userId, challengeId);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 제안이 삭제되었습니다.", null));
    }

    /**
     * 4. 소비 목표 챌린지 승인 / 반려 (부모 전용)
     */
    @Operation(
            summary = "소비 목표 챌린지 승인 / 반려",
            description = "부모가 PENDING 상태인 자녀의 챌린지를 APPROVED 또는 REJECTED로 처리합니다. 가족 관계가 일치하는 자녀의 챌린지만 처리할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "챌린지 상태 변경 성공")
    @PatchMapping("/{challengeId}/status")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.StatusUpdateResponse>> updateChallengeStatus(
            @Parameter(description = "상태를 변경할 챌린지 ID") @PathVariable UUID challengeId,
            @RequestBody SpendingChallengeDto.StatusUpdateRequest request,
            @AuthenticationPrincipal UUID parentId) {

        SpendingChallengeDto.StatusUpdateResponse response = spendingChallengeService.updateChallengeStatus(parentId, challengeId, request);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 상태가 업데이트되었습니다.", response));
    }

    /**
     * 5. 다음 주 소비 목표 챌린지 목록 조회 (부모 / 자녀 공통)
     */
    @Operation(
            summary = "다음 주 소비 목표 챌린지 목록 조회",
            description = "다음 주 월요일 기준의 소비 목표 챌린지 목록을 조회합니다. 자녀는 본인 목록을 조회하고, 부모는 childId를 필수로 전달하여 특정 자녀의 목록을 조회합니다. status 파라미터로 상태 필터링이 가능합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @GetMapping
    public ResponseEntity<ApiResponse<SpendingChallengeDto.ListResponse>> getNextWeekChallenges(
            @Parameter(description = "조회할 자녀의 ID (부모 호출 시 필수)") @RequestParam(required = false) UUID childId,
            @Parameter(description = "챌린지 상태 필터 (PENDING / APPROVED / REJECTED 등, 미전달 시 전체 조회)") @RequestParam(required = false) ChallengeStatus status,
            @AuthenticationPrincipal UUID requestUserId) {

        SpendingChallengeDto.ListResponse response = spendingChallengeService.getNextWeekChallenges(requestUserId, childId, status);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 목록을 불러왔습니다.", response));
    }

    /**
     * 6. 소비 목표 챌린지 단건 상세 조회 (부모 / 자녀 공통)
     */
    @Operation(
            summary = "소비 목표 챌린지 단건 상세 조회",
            description = "챌린지 ID로 단건 상세 정보를 조회합니다. 자녀는 본인 챌린지만, 부모는 같은 가족의 자녀 챌린지만 조회할 수 있습니다. 챌린지 기간 내 실시간 누적 소비액도 함께 반환됩니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 조회 성공")
    @GetMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.DetailResponse>> getChallengeDetail(
            @Parameter(description = "조회할 챌린지 ID") @PathVariable UUID challengeId,
            @AuthenticationPrincipal UUID requestUserId) {

        SpendingChallengeDto.DetailResponse response = spendingChallengeService.getChallengeDetail(requestUserId, challengeId);

        return ResponseEntity.ok(ApiResponse.success("소비 목표 챌린지 상세 정보를 불러왔습니다.", response));
    }

    /**
     * 7. 미수령 보상 목록 조회 (자녀 전용)
     */
    @Operation(
            summary = "미수령 보상 목록 조회",
            description = "자녀가 보상 요청 가능한 챌린지 목록을 조회합니다. 지난주 일요일을 endDate로 하는 SUCCESS 상태 챌린지만 반환됩니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "미수령 보상 목록 조회 성공")
    @GetMapping("/unclaimed")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.UnclaimedListResponse>> getUnclaimedChallenges(
            @AuthenticationPrincipal UUID userId) {

        SpendingChallengeDto.UnclaimedListResponse response = spendingChallengeService.getUnclaimedChallenges(userId);

        return ResponseEntity.ok(ApiResponse.success("보상 요청 가능한 챌린지 목록을 불러왔습니다.", response));
    }

    /**
     * 8. 보상 요청 (자녀 전용)
     */
    @Operation(
            summary = "보상 요청",
            description = "자녀가 SUCCESS 상태인 지난주 챌린지에 대해 보상을 요청합니다. 요청 후 챌린지 상태가 REWARD_REQUESTED로 변경되고 부모에게 FCM 알림이 전송됩니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "보상 요청 성공")
    @PostMapping("/{challengeId}/reward")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.RewardRequestResponse>> requestReward(
            @Parameter(description = "보상을 요청할 챌린지 ID") @PathVariable UUID challengeId,
            @AuthenticationPrincipal UUID userId) {

        SpendingChallengeDto.RewardRequestResponse response = spendingChallengeService.requestReward(userId, challengeId);

        return ResponseEntity.ok(ApiResponse.success("보상 요청이 완료되었습니다. 부모님께 알림을 전송했습니다.", response));
    }

    /**
     * 9. 보상 송금 처리 (부모 전용)
     */
    @Operation(
            summary = "보상 송금 처리",
            description = "부모가 REWARD_REQUESTED 상태인 챌린지에 대해 자녀에게 실제 계좌 이체를 수행합니다. 송금 완료 후 챌린지 상태가 REWARDED로 변경되고 자녀에게 FCM 알림이 전송됩니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "보상 송금 성공")
    @PostMapping("/{challengeId}/reward-transfer")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.RewardTransferResponse>> processRewardTransfer(
            @Parameter(description = "보상 송금을 처리할 챌린지 ID") @PathVariable UUID challengeId,
            @AuthenticationPrincipal UUID parentId,
            @RequestBody SpendingChallengeDto.RewardTransferRequest request) {

        SpendingChallengeDto.RewardTransferResponse response =
                spendingChallengeService.processRewardTransfer(parentId, challengeId, request);

        return ResponseEntity.ok(ApiResponse.success("보상 송금이 완료되었습니다.", response));
    }

    /**
     * 10. 이번 주 소비 목표 챌린지 목록 조회 (부모 전용)
     */
    @Operation(
            summary = "이번 주 소비 목표 챌린지 목록 조회 (부모 전용)",
            description = "부모가 특정 자녀의 이번 주(현재 진행 중인 주) 소비 목표 챌린지 전체 목록을 조회합니다. childId는 필수이며 가족 관계가 일치하는 자녀만 조회할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이번 주 챌린지 목록 조회 성공")
    @GetMapping("/this-week")
    public ResponseEntity<ApiResponse<SpendingChallengeDto.ListResponse>> getThisWeekChallengesForParent(
            @Parameter(description = "조회할 자녀의 ID (필수)") @RequestParam UUID childId,
            @AuthenticationPrincipal UUID parentId) {

        SpendingChallengeDto.ListResponse response =
                spendingChallengeService.getThisWeekChallengesForParent(parentId, childId);

        return ResponseEntity.ok(ApiResponse.success("이번 주 소비 목표 챌린지 목록을 불러왔습니다.", response));
    }
}