package com.akku.backend.domain.auth.service;

import com.akku.backend.domain.auth.dto.KakaoUserInfo;
import com.akku.backend.domain.auth.dto.RefreshData;
import com.akku.backend.domain.auth.dto.SignupData;
import com.akku.backend.domain.auth.dto.SignupRequest;
import com.akku.backend.domain.auth.dto.SocialLoginData;
import com.akku.backend.domain.auth.entity.LogoutToken;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.LogoutTokenRepository;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.global.security.JwtProvider;
import com.akku.backend.domain.auth.exception.AuthErrorCode;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoService kakaoService;
    private final SsafyFinanceService ssafyFinanceService;
    private final UserRepository userRepository;
    private final LogoutTokenRepository logoutTokenRepository;
    private final JwtProvider jwtProvider;

    /**
     * 카카오 소셜 로그인 처리
     * - 신규 유저: 금융망 계정 생성 → tempToken 발급
     * - 기존 유저: JWT Access + Refresh 토큰 발급
     */
    @Transactional
    public SocialLoginData kakaoLogin(String socialToken) {
        KakaoUserInfo kakaoUserInfo = kakaoService.getUserInfo(socialToken);
        String email = kakaoUserInfo.getEmail();
        String providerId = kakaoUserInfo.getProviderId();
        String nickname = kakaoUserInfo.getNickname();

        log.info("카카오 로그인 시도 - email: {}, providerId: {}", email, providerId);

        boolean[] isNewUserFlag = {false};
        User user = userRepository
                .findByProviderAndProviderId("KAKAO", providerId)
                .orElseGet(() -> {
                    isNewUserFlag[0] = true;
                    // 이메일이 없는 경우 providerId를 이메일 형식으로 변환하여 사용
                    String effectiveId = (email != null) ? email : providerId + "@kakao.com";
                    String userKey = ssafyFinanceService.createMember(effectiveId);
                    User newUser = User.builder()
                            .email(effectiveId)
                            .provider("KAKAO")
                            .providerId(providerId)
                            .userKey(userKey)
                            .name(nickname != null ? nickname : "사용자")
                            .role("PARENT")
                            .build();
                    return userRepository.save(newUser);
                });

        // 탈퇴/비활성 사용자 차단
        if (!user.getIsActive()) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        boolean isNewUser = isNewUserFlag[0];

        if (isNewUser) {
            String tempToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
            log.info("신규 유저 tempToken 발급 - userId: {}", user.getId());
            return SocialLoginData.newUser(tempToken);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        log.info("기존 유저 JWT 발급 - userId: {}", user.getId());

        return SocialLoginData.existingUser(accessToken, refreshToken);
    }

    /**
     * 신규 계정 가입 1단계: role + name 저장 → signupToken 발급
     */
    @Transactional
    public SignupData signup(UUID userId, SignupRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.name(), request.role());

        String signupToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        log.info("signup 완료 - userId: {}, role: {}", userId, request.role());

        return new SignupData(signupToken);
    }

    /**
     * 로그아웃: Access Token을 블랙리스트에 등록
     */
    @Transactional
    public void logout(String accessToken, UUID userId) {
        if (accessToken == null) {
            throw new ApiException(AuthErrorCode.INVALID_TOKEN);
        }
        Claims claims = jwtProvider.parseToken(accessToken);
        LocalDateTime expiredAt = claims.getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        LogoutToken logoutToken = LogoutToken.builder()
                .token(accessToken)
                .userId(userId)
                .expiredAt(expiredAt)
                .createdAt(LocalDateTime.now())
                .build();

        logoutTokenRepository.save(logoutToken);
        log.info("로그아웃 완료 - userId: {}", userId);
    }

    /**
     * 토큰 재발급: Refresh Token 검증 후 새로운 Access Token 발급
     */
    public RefreshData refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new ApiException(AuthErrorCode.TOKEN_EXPIRED);
        }

        UUID userId = jwtProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 탈퇴/비활성 사용자 차단
        if (!user.getIsActive()) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        log.info("토큰 재발급 완료 - userId: {}", userId);

        return new RefreshData(newAccessToken);
    }
}
