package com.akku.backend.domain.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
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
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("SSAFY_API_KEY가 설정되지 않았습니다. 금융망 관련 기능이 정상 작동하지 않을 수 있습니다.");
        }
    }

    /**
     * 금융망에 사용자 계정 생성
     * @param email 카카오 이메일 (userId로 사용)
     * @return userKey (이후 금융 API 호출 시 사용)
     */
    public String createMember(String userId) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("apiKey", apiKey);
            requestBody.put("userId", userId != null ? userId : "unknown");

            Map<?, ?> response = restClient.post()
                    .uri("/member")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("userKey")) {
                String userKey = (String) response.get("userKey");
                log.info("금융망 계정 생성 성공 - userId: [MASKED], userKey: [MASKED]");
                return userKey;
            }

            throw new RuntimeException("금융망 응답에 userKey 없음");

        } catch (HttpClientErrorException.Conflict | HttpClientErrorException.BadRequest e) {
            // 이미 존재하는 ID인 경우, 기존 userKey를 조회
            log.warn("금융망에 이미 존재하는 식별자: {} - 기존 userKey 조회 시도", userId);
            return searchMember(userId);
        }
    }

    /**
     * 금융망에서 기존 사용자의 userKey 조회
     */
    private String searchMember(String userId) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("apiKey", apiKey);
            requestBody.put("userId", userId);

            Map<?, ?> response = restClient.post()
                    .uri("/member/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("userKey")) {
                String userKey = (String) response.get("userKey");
                log.info("금융망 기존 계정 조회 성공 - userId: [MASKED], userKey: [MASKED]");
                return userKey;
            }

            throw new RuntimeException("금융망 기존 계정 조회 실패 - userKey 없음");
        } catch (Exception e) {
            log.error("금융망 기존 계정 조회 실패 - userId: {}", userId, e);
            throw new RuntimeException("금융망 계정 조회 실패", e);
        }
    }
}
