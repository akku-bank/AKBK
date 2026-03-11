package com.akku.backend.domain.auth.service;

import com.akku.backend.domain.auth.dto.KakaoUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class KakaoService {

    private final RestClient restClient;
    private final String userInfoUri;

    public KakaoService(
            @Value("${kakao.user-info-uri}") String userInfoUri
    ) {
        this.restClient = RestClient.create();
        this.userInfoUri = userInfoUri;
    }

    /**
     * 카카오 액세스 토큰으로 사용자 정보 조회
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        return restClient.get()
                .uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .body(KakaoUserInfo.class);
    }
}
