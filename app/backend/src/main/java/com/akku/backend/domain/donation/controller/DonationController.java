package com.akku.backend.domain.donation.controller;

import com.akku.backend.domain.donation.dto.*;
import com.akku.backend.domain.donation.service.DonationService;
import com.akku.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jelling-hub")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    /**
     * 젤링 허브 홈 통합 정보 조회
     */
    @GetMapping
    public ApiResponse<JellingHubResponse> getJellingHubInfo(
            @AuthenticationPrincipal UUID userId) {
        return ApiResponse.success("젤링 허브 정보 조회가 완료되었습니다.", donationService.getJellingHubInfo(userId));
    }

    /**
     * 기부처 목록 조회
     */
    @GetMapping("/charities")
    public ApiResponse<CharityListResponse> getCharityList() {
        return ApiResponse.success("기부처 목록 조회가 완료되었습니다.", donationService.getCharityList());
    }

    /**
     * 기부 목표 설정
     */
    @PostMapping("/active-charity")
    public ApiResponse<SetTargetCharityResponse> setTargetCharity(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SetTargetCharityRequest request) {
        UUID activeCharityId = donationService.setTargetCharity(userId, request.charityId());
        return ApiResponse.success("기부 목표 설정이 완료되었습니다.", new SetTargetCharityResponse(activeCharityId));
    }


    /**
     * 완료된 저금통에 대해 보상 수령
     */
    @PostMapping("/donations/rewards")
    public ApiResponse<ReceiveRewardResponse> receiveReward(
            @AuthenticationPrincipal UUID userId) {
        return ApiResponse.success("보상 수령이 완료되었습니다.", donationService.receiveReward(userId));
    }
}
