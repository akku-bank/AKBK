package com.akku.backend.domain.challenge.controller;

import com.akku.backend.domain.challenge.dto.EsgChallengeDto;
import com.akku.backend.domain.challenge.service.EsgChallengeService;
import com.akku.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/challenges/esg")
@RequiredArgsConstructor
public class EsgChallengeController {

    private final EsgChallengeService esgChallengeService;

    // 이번 주 ESG 챌린지 상태 조회 (자녀 전용) — 없으면 Lazy Insert
    @GetMapping
    public ResponseEntity<ApiResponse<EsgChallengeDto.StatusResponse>> getThisWeekChallenge(
            @AuthenticationPrincipal UUID userId) {

        EsgChallengeDto.StatusResponse response = esgChallengeService.getThisWeekChallenge(userId);

        return ResponseEntity.ok(ApiResponse.success("이번 주 ESG 챌린지 정보를 불러왔습니다.", response));
    }

    // ESG 챌린지 보상 수령 (자녀 전용) — SUCCESS 상태에서만 가능
    @PostMapping("/{challengeId}/rewards")
    public ResponseEntity<ApiResponse<EsgChallengeDto.RewardResponse>> receiveReward(
            @PathVariable UUID challengeId,
            @AuthenticationPrincipal UUID userId) {

        EsgChallengeDto.RewardResponse response = esgChallengeService.receiveReward(userId, challengeId);

        return ResponseEntity.ok(ApiResponse.success("ESG 챌린지 보상이 지급되었습니다.", response));
    }
}
