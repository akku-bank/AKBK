package com.akku.backend.domain.notification.controller;

import com.akku.backend.domain.notification.dto.NotificationRequest;
import com.akku.backend.domain.notification.service.FcmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 관련 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @Operation(summary = "알림 테스트 전송", description = "FCM 토큰을 이용해 테스트 알림을 보냅니다.")
    @PostMapping("/test")
    public ResponseEntity<String> sendTestNotification(@Valid @RequestBody NotificationRequest request) {
        fcmService.sendMessage(request);
        return ResponseEntity.ok("알림 전송 성공");
    }
}
