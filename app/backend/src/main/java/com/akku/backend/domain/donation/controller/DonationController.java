package com.akku.backend.domain.donation.controller;

import com.akku.backend.domain.donation.dto.CharityResponse;
import com.akku.backend.domain.donation.dto.SetTargetCharityRequest;
import com.akku.backend.domain.donation.service.DonationService;
import com.akku.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jelling-hub/charities")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    /**
     * 기부처 목록 조회
     */
    @GetMapping
    public ApiResponse<List<CharityResponse>> getCharityList() {
        return ApiResponse.success("기부처 목록 조회가 완료되었습니다.", donationService.getCharityList());
    }

    /**
     * 기부 목표 설정
     */
    @PostMapping
    public ApiResponse<Void> setTargetCharity(
            @AuthenticationPrincipal UUID userId,
            @RequestBody SetTargetCharityRequest request) {
        donationService.setTargetCharity(userId, request.charityId());
        return ApiResponse.success("기부 목표 설정이 완료되었습니다.");
    }
}
