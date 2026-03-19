package com.akku.backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path:}")
    private String firebaseConfigPath;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(firebaseConfigPath)) {
            log.warn("FIREBASE_CONFIG_PATH가 설정되지 않아 Firebase 초기화를 건너뜁니다. FCM 기능이 비활성화됩니다.");
            return;
        }

        ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
        if (!resource.exists()) {
            log.warn("Firebase 설정 파일을 찾을 수 없습니다 (path={}). Firebase 초기화를 건너뜁니다.", firebaseConfigPath);
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase application has been initialized");
            }
        } catch (IOException e) {
            log.warn("Firebase 초기화 실패 — FCM 기능이 비활성화됩니다. 원인: {}", e.getMessage());
        }
    }
}
