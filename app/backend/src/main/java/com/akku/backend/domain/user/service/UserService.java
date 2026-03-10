package com.akku.backend.domain.user.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.user.dto.UserProfileResponse;
import com.akku.backend.domain.user.dto.UserUpdateRequest;
import com.akku.backend.domain.user.dto.UserUpdateResponse;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 내 정보 조회
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        return new UserProfileResponse(
                user.getId(),
                user.getRole(),
                user.getName(),
                user.getFamilyId(),
                user.getLevel()
        );
    }

    /**
     * 내 정보 수정 (이름, FCM 토큰)
     */
    @Transactional
    public UserUpdateResponse updateProfile(UUID userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        if (request.name() != null) {
            user.updateName(request.name());
        }

        log.info("프로필 수정 완료 - userId: {}, name: {}", userId, user.getName());
        return new UserUpdateResponse(user.getName());
    }

    /**
     * FCM 토큰 갱신
     */
    @Transactional
    public void updateFcmToken(UUID userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        user.updateFcmToken(fcmToken);
        log.info("FCM 토큰 갱신 완료 - userId: {}", userId);
    }

    /**
     * 회원 탈퇴 (Soft Delete)
     * is_active = false 처리
     */
    @Transactional
    public void withdraw(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        user.deactivate();
        log.info("회원 탈퇴 완료 (soft delete) - userId: {}", userId);
    }
}
