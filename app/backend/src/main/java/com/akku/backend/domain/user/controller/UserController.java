package com.akku.backend.domain.user.controller;

import com.akku.backend.domain.auth.dto.ApiResponse;
import com.akku.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원 탈퇴 (Soft Delete)
     * DELETE /api/users/me
     * Header: Authorization: Bearer {token}
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UUID userId
    ) {
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.ok(
                "회원 탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.",
                null
        ));
    }
}
