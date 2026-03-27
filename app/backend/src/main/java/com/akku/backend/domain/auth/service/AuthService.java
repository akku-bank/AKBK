package com.akku.backend.domain.auth.service;

import com.akku.backend.domain.auth.dto.KakaoUserInfo;
import com.akku.backend.domain.auth.dto.LoginRequest;
import com.akku.backend.domain.auth.dto.RefreshData;
import com.akku.backend.domain.auth.dto.SignupData;
import com.akku.backend.domain.auth.dto.SignupPinData;
import com.akku.backend.domain.auth.dto.SignupPinRequest;
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

import java.time.Clock;
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
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final Clock clock;

    /**
     * 카카오 소셜 로그인 처리
     * - 신규 유저: 금융망 계정 생성 → tempToken 발급
     * - 기존 유저: JWT Access + Refresh 토큰 발급
     */
    @Transactional
    public SocialLoginData kakaoLogin(String socialToken, String fcmToken) {
        KakaoUserInfo kakaoUserInfo = kakaoService.getUserInfo(socialToken);
        String providerId = kakaoUserInfo.getProviderId();
        String nickname = kakaoUserInfo.getNickname();

        log.info("카카오 로그인 시도 - providerId: [MASKED]");

        boolean[] isNewUserFlag = {false};
        User user = userRepository
                .findByProviderAndProviderId("KAKAO", providerId)
                .orElseGet(() -> {
                    isNewUserFlag[0] = true;
                    // 이메일이 없는 경우 providerId를 이메일 형식으로 변환하여 사용
                    String effectiveId =  providerId + "@kakao.com";
                    String userKey = ssafyFinanceService.createMember(effectiveId);
                    User newUser = User.builder()
                            .provider("KAKAO")
                            .providerId(providerId)
                            .userKey(userKey)
                            .name(nickname != null ? nickname : "사용자")
                            .role("PARENT")
                            .fcmToken(fcmToken)
                            .build();
                    return userRepository.save(newUser);
                });
        
        // DB에는 있으나 간편 비밀번호가 없는 경우 신규 가입 절차로 유도
        if (user.getPinPassword() == null) {
            isNewUserFlag[0] = true;
        }

        // 탈퇴/비활성 사용자 차단
        if (!user.getIsActive()) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        // 기존 유저: 로그인 시 FCM 토큰 갱신
        if (!isNewUserFlag[0] && fcmToken != null) {
            user.updateFcmToken(fcmToken);
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

        user.updateProfile(request.name(), request.role(), request.birthDate());

        String signupToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        log.info("signup 1단계 완료(프로필 저장) - userId: {}, role: {}", userId, request.role());

        return new SignupData(signupToken);
    }

    /**
     * 신규 계정 가입 2단계: PIN 설정 → 최종 JWT 토큰 발급
     */
    @Transactional
    public SignupPinData signupPin(UUID userId, SignupPinRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // PIN 암호화 저장
        String encodedPin = passwordEncoder.encode(request.pin());
        user.updatePinPassword(encodedPin);

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        log.info("signup 2단계 완료(PIN 설정) - userId: {}", userId);

        return new SignupPinData(accessToken, refreshToken);
    }

    /**
     * 간편 로그인: userId와 pin 확인 후 JWT 토큰 발급
     */
    @Transactional
    public SocialLoginData login(LoginRequest request) {
        // userId(UUID 형식) 기반 사용자 조회
        UUID uuid;
        try {
            uuid = UUID.fromString(request.userId());
        } catch (IllegalArgumentException e) {
            throw new ApiException(AuthErrorCode.LOGIN_FAILED);
        }

        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (user.getPinPassword() == null || !passwordEncoder.matches(request.pin(), user.getPinPassword())) {
            throw new ApiException(AuthErrorCode.PIN_MISMATCH);
        }

        // 탈퇴/비활성 사용자 차단
        if (!user.getIsActive()) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        // FCM 토큰 갱신
        if (request.fcmToken() != null) {
            user.updateFcmToken(request.fcmToken());
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        log.info("간편 로그인 성공 - userId: {}", user.getId());

        return SocialLoginData.existingUser(accessToken, refreshToken);
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
                .atZone(clock.getZone())
                .toLocalDateTime();

        LogoutToken logoutToken = LogoutToken.builder()
                .token(accessToken)
                .userId(userId)
                .expiredAt(expiredAt)
                .createdAt(LocalDateTime.now(clock))
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

    /**
     * 개발용 우회 로그인: 유저 ID만으로 JWT 발급
     */
    @Transactional(readOnly = true)
    public SocialLoginData testLogin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new ApiException(AuthErrorCode.ACCESS_DENIED);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        log.info("개발용 우회 로그인 성공 - userId: {}", user.getId());

        return SocialLoginData.existingUser(accessToken, refreshToken);
    }
}
