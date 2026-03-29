package com.akku.backend.domain.notification.event;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.notification.dto.NotificationRequest;
import com.akku.backend.domain.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JellingNotificationListener {

    private final UserRepository userRepository;
    private final FcmService fcmService;

    public void sendJellingRewardNotification(UUID userId, long amount, String description) {
        log.debug("젤링 보상 알림 처리 — userId: {}, amount: {}, desc: {}", userId, amount, description);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getFcmToken() == null || user.getFcmToken().isBlank()) {
            return;
        }

        try {
            fcmService.sendMessage(NotificationRequest.builder()
                    .token(user.getFcmToken())
                    .title("젤링이 도착했어요! \uD83D\uDDF3\uFE0F")
                    .body(String.format("%s 보상으로 젤링 %,d개가 적립되었습니다.", description, amount))
                    .build());
            log.info("젤링 FCM 알림 발송 성공 — userId: {}", userId);
        } catch (Exception e) {
            log.error("젤링 FCM 알림 발송 실패 — userId: {}", userId, e);
        }
    }
}
