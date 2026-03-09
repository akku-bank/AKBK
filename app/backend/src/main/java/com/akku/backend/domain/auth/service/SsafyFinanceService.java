package com.akku.backend.domain.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class SsafyFinanceService {

    private final RestClient restClient;
    private final String apiKey;

    public SsafyFinanceService(
            @Value("${ssafy.api.base-url}") String baseUrl,
            @Value("${ssafy.api.key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
    }

    /**
     * 금융망에 사용자 계정 생성
     * @param email 카카오 이메일 (userId로 사용)
     * @return userKey (이후 금융 API 호출 시 사용)
     */
    public String createMember(String email) {
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/member")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "apiKey", apiKey,
                            "userId", email
                    ))
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("userKey")) {
                String userKey = (String) response.get("userKey");
                log.info("금융망 계정 생성 성공 - email: {}, userKey: {}", email, userKey);
                return userKey;
            }

            throw new RuntimeException("금융망 응답에 userKey 없음");

        } catch (HttpClientErrorException.Conflict e) {
            log.warn("금융망에 이미 존재하는 이메일: {}", email);
            throw new RuntimeException("이미 금융망에 등록된 이메일입니다.");
        }
    }
}
