package com.akku.backend.domain.challenge.controller;

import com.akku.backend.domain.challenge.dto.WeeklyChallengesDto;
import com.akku.backend.domain.challenge.facade.WeeklyChallengesFacade;
import com.akku.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * GET /api/challenges — 이번 주 전체 챌린지 종합 조회 (자녀 전용).
 *
 * <p>기존 컨트롤러와 경로 충돌 없음:
 * <ul>
 *   <li>SpendingChallengeController → /api/challenges/spending</li>
 *   <li>EsgChallengeController      → /api/challenges/esg</li>
 *   <li>WeeklyChallengesController  → /api/challenges  (GET 단독)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class WeeklyChallengesController {

    private final WeeklyChallengesFacade weeklyChallengesFacade;

    /**
     * 이번 주 ESG / 퀴즈 / 소비 챌린지 상태를 한 번에 조회한다.
     * 권한: CHILD (자녀 본인) — 인증 토큰의 userId 기준으로 조회.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WeeklyChallengesDto.Response>> getWeeklyChallenges(
            @AuthenticationPrincipal UUID userId) {

        WeeklyChallengesDto.Response response = weeklyChallengesFacade.getWeeklyChallenges(userId);

        return ResponseEntity.ok(ApiResponse.success("금주 전체 챌린지 상태를 조회했습니다.", response));
    }
}
