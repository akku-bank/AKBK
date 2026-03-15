package com.akku.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${external.fastapi.base-url}")
    private String fastApiBaseUrl;

    @Bean
    public RestClient fastApiClient() {
        // 1. Java 11 내장 HttpClient 생성 및 Connect Timeout 설정
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)) // FastAPI 서버 연결 최대 3초 대기
                .build();

        // 2. RequestFactory에 HttpClient 주입 및 Read Timeout 설정
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15)); // AI 모델 추론 및 응답 최대 15초 대기

        // 3. RestClient 빌드
        return RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // 공통 에러 핸들링
                .defaultStatusHandler(
                        statusCode -> statusCode.is4xxClientError() || statusCode.is5xxServerError(),
                        (request, response) -> {
                            // TODO: 우리 프로젝트의 글로벌 예외 처리(CustomException)로 변경 필요
                            throw new RuntimeException("FastAPI 서버 통신 중 오류가 발생했습니다. 상태 코드: " + response.getStatusCode());
                        })
                .build();
    }
}