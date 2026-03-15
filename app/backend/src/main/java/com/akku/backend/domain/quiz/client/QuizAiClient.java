package com.akku.backend.domain.quiz.client;

import com.akku.backend.domain.quiz.dto.ChatRequest;
import com.akku.backend.domain.quiz.dto.ChatResponse;
import com.akku.backend.domain.quiz.exception.QuizErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * FastAPI AI 서버와의 통신을 담당하는 클라이언트 컴포넌트.
 *
 * <p>RestClient의 플루언트 API를 단일 메서드로 캡슐화하여
 * 서비스 계층의 테스트 가능성(testability)을 확보한다.</p>
 */
@Component
@RequiredArgsConstructor
public class QuizAiClient {

    private final RestClient fastApiClient;

    /**
     * FastAPI /quiz/chat 엔드포인트에 힌트 요청을 전송하고 응답을 반환한다.
     *
     * @param request 사용자 힌트 요청 (quizId + 메시지)
     * @return AI 응답 (reply + 전체 대화 기록 chatJson)
     * @throws ApiException AI 서버 통신 오류 시 {@link QuizErrorCode#AI_SERVER_ERROR}
     */
    public ChatResponse requestHint(ChatRequest request) {
        try {
            ChatResponse response = fastApiClient
                    .post()
                    .uri("/quiz/chat")
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

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
}
