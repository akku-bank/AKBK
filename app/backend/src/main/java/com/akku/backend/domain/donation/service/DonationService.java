package com.akku.backend.domain.donation.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.donation.dto.CharityResponse;
import com.akku.backend.domain.donation.entity.ActiveCharity;
import com.akku.backend.domain.donation.entity.Charity;
import com.akku.backend.domain.donation.exception.DonationErrorCode;
import com.akku.backend.domain.donation.repository.ActiveCharityRepository;
import com.akku.backend.domain.donation.repository.CharityRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {

    private final CharityRepository charityRepository;
    private final ActiveCharityRepository activeCharityRepository;
    private final UserRepository userRepository;

    /**
     * 전체 기부처 목록 조회
     */
    public List<CharityResponse> getCharityList() {
        return charityRepository.findAll().stream()
                .map(charity -> new CharityResponse(
                        charity.getId(),
                        charity.getName(),
                        charity.getTargetAmount(),
                        charity.getDescription()
                ))
                .toList();
    }

    /**
     * 기부 목표 설정
     */
    @Transactional
    public void setTargetCharity(UUID userId, UUID charityId) {
        // 진행 중인 기부 목표가 있는지 확인
        if (activeCharityRepository.existsByUserIdAndStatus(userId, "IN_PROGRESS")) {
            throw new ApiException(DonationErrorCode.ACTIVE_CHARITY_ALREADY_EXISTS);
        }

        // 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 기부처 존재 확인
        Charity charity = charityRepository.findById(charityId)
                .orElseThrow(() -> new ApiException(DonationErrorCode.CHARITY_NOT_FOUND));

        // 새로운 기부 목표 저장
        ActiveCharity activeCharity = ActiveCharity.builder()
                .user(user)
                .charity(charity)
                .currentAmount(0L)
                .status("IN_PROGRESS")
                .build();

        activeCharityRepository.save(activeCharity);
    }
}
