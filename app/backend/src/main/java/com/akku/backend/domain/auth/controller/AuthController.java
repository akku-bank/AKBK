package com.akku.backend.domain.auth.controller;

import com.akku.backend.domain.auth.dto.KakaoLoginRequest;
import com.akku.backend.domain.auth.dto.KakaoUserInfo;
import com.akku.backend.domain.auth.dto.RefreshData;
import com.akku.backend.domain.auth.dto.RefreshRequest;
import com.akku.backend.domain.auth.dto.SignupData;
import com.akku.backend.domain.auth.dto.SignupRequest;
import com.akku.backend.domain.auth.dto.SocialLoginData;
import com.akku.backend.global.dto.ApiResponse;
import com.akku.backend.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 소셜 로그인
     * POST /api/auth/social/{provider}
     * Body: { "socialToken": "카카오_액세스_토큰" }
     */
    @Operation(summary = "소셜 로그인", description = "카카오 액세스 토큰으로 로그인을 시도합니다.")
    @PostMapping("/social/{provider}")
    public ResponseEntity<ApiResponse<SocialLoginData>> socialLogin(
            @PathVariable String provider,
            @Valid @RequestBody KakaoLoginRequest request
    ) {
        SocialLoginData data = authService.kakaoLogin(request.socialToken(), request.fcmToken());

        String message = data.isRegistered()
                ? "로그인에 성공했습니다."
                : "신규 유저입니다. 회원가입을 진행해주세요.";

        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * 신규 계정 가입 1단계: role + name 등록
     * POST /api/auth/signup
     * Header: Authorization: Bearer {tempToken}
     * Body: { "role": "PARENT", "name": "홍길동" }
     */
    @Operation(summary = "회원가입 1단계", description = "역할(부모/자녀)과 이름을 등록합니다. 완료 후 signupToken이 발급됩니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupData>> signup(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SignupRequest request
    ) {
        SignupData data = authService.signup(userId, request);
        return ResponseEntity.ok(ApiResponse.success(
                "기본 정보 등록이 완료되었습니다. 간편 비밀번호를 설정해주세요.",
                data
        ));
    }

    /**
     * 신규 계정 가입 2단계: PIN 설정
     * POST /api/auth/signup/pin
     * Header: Authorization: Bearer {signupToken}
     * Body: { "pin": "123456" }
     */
    @Operation(summary = "PIN 설정", description = "6자리 간편 비밀번호를 설정하고 회원가입을 완료합니다.")
    @PostMapping("/signup/pin")
    public ResponseEntity<ApiResponse<com.akku.backend.domain.auth.dto.SignupPinData>> signupPin(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody com.akku.backend.domain.auth.dto.SignupPinRequest request
    ) {
        com.akku.backend.domain.auth.dto.SignupPinData data = authService.signupPin(userId, request);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 최종 완료되었습니다.", data));
    }

    /**
     * 간편 로그인
     * POST /api/auth/login
     * Body: { "userId": "uuid-or-email", "pin": "123456" }
     */
    @Operation(summary = "간편 로그인", description = "사용자 ID와 PIN으로 로그인을 시도합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SocialLoginData>> login(
            @Valid @RequestBody com.akku.backend.domain.auth.dto.LoginRequest request
    ) {
        SocialLoginData data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", data));
    }

    /**
     * 로그아웃
     * POST /api/auth/logout
     * Header: Authorization: Bearer {accessToken}
     */
    @Operation(summary = "로그아웃", description = "액세스 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UUID userId,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String token = resolveToken(request);
        authService.logout(token, userId);
        return ResponseEntity.ok(ApiResponse.success("성공적으로 로그아웃 되었습니다.", null));
    }

    private String resolveToken(jakarta.servlet.http.HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * 토큰 재발급
     * POST /api/auth/refresh
     * Body: { "refreshToken": "기존_refresh_token" }
     */
    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshData>> refresh(
            @RequestBody RefreshRequest request
    ) {
        RefreshData data = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", data));
    }
}
