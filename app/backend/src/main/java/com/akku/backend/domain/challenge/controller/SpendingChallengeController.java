package com.akku.backend.domain.challenge.controller;

import com.akku.backend.domain.challenge.dto.SpendingChallengeDto;
import com.akku.backend.domain.challenge.service.SpendingChallengeService;
import com.akku.backend.global.dto.ApiResponse; // 팀에서 쓰는 공통 응답 포맷이라 가정
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
}