package com.akku.backend.domain.quiz.client;

import com.akku.backend.domain.quiz.dto.ChatResponse;
import com.akku.backend.domain.quiz.event.QuizChatEvent;
import com.akku.backend.domain.quiz.exception.QuizErrorCode;
import com.akku.backend.global.error.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@Slf4j
public class QuizAiClient {

    private final String fastApiBaseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public QuizAiClient(
            @Value("${external.fastapi.base-url}") String fastApiBaseUrl,
            ObjectMapper objectMapper
    ) {
        this.fastApiBaseUrl = fastApiBaseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public ChatResponse requestHint(QuizChatEvent request, String chatJson) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("event_type", request.eventType());
            payload.put("event_id", request.eventId().toString());
            payload.put("user_id", request.userId().toString());
            payload.put("quiz_id", request.quizId().toString());
            payload.put("message", request.message());
            payload.put("remaining_credits", request.remainingCredits());
            payload.put("difficulty", request.difficulty().toLowerCase());
            payload.put("birth_date", request.birthDate().toString());
            if (chatJson == null) {
                payload.putNull("chat_json");
            } else {
                payload.put("chat_json", chatJson);
            }
            String payloadJson = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(resolveChatUri()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            String responseBody = httpResponse.body();

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                log.warn("FastAPI chat request failed. status={}, responseBody={}", httpResponse.statusCode(), responseBody);
                throw new ApiException(QuizErrorCode.AI_SERVER_ERROR);
            }

            ChatResponse response = objectMapper.readValue(responseBody, ChatResponse.class);
            if (response == null) {
                throw new ApiException(QuizErrorCode.AI_SERVER_ERROR);
            }
            return response;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(QuizErrorCode.AI_SERVER_ERROR);
        }
    }

    private String resolveChatUri() {
        return fastApiBaseUrl.endsWith("/")
                ? fastApiBaseUrl + "v1/chat/message"
                : fastApiBaseUrl + "/v1/chat/message";
    }
}
