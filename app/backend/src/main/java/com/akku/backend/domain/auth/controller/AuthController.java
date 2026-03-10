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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
     * 로그아웃
     * POST /api/auth/logout
     * Header: Authorization: Bearer {accessToken}
     */
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
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshData>> refresh(
            @RequestBody RefreshRequest request
    ) {
        RefreshData data = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", data));
    }
}
