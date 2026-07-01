package com.akku.backend.domain.notification.service;

import com.akku.backend.domain.notification.dto.NotificationRequest;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    public void sendMessage(NotificationRequest request) {
        try {
            Message message = Message.builder()
                    .setToken(request.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(request.getTitle())
                            .setBody(request.getBody())
                            .setImage(request.getImageUrl())
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH) // 긴급 메시지로 설정하여 헤드업 노출
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("default") // Expo에서 생성한 채널과 일치
                                    .setPriority(AndroidNotification.Priority.MAX)
                                    .build())
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent message: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message", e);
            throw new RuntimeException("알림 전송 중 오류가 발생했습니다.", e);
        }
    }
}
