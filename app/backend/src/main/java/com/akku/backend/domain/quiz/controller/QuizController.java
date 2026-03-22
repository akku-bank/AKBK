package com.akku.backend.domain.quiz.controller;

import com.akku.backend.domain.quiz.dto.AnswerRequest;
import com.akku.backend.domain.quiz.dto.AnswerResponse;
import com.akku.backend.domain.quiz.dto.ChatRequest;
import com.akku.backend.domain.quiz.dto.QuizResponse;
import com.akku.backend.domain.quiz.service.QuizService;
import com.akku.backend.domain.quiz.sse.SseConnectionManager;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * 금융 퀴즈 챌린지 API
 */
@Tag(name = "Quiz Challenge API", description = "금융 퀴즈 조회, AI 힌트, 정답 제출")
@RestController
@RequestMapping("/api/challenges/quizzes")
@PreAuthorize("hasRole('CHILD')")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final SseConnectionManager sseConnectionManager;

    /**
     * 1. 오늘의 퀴즈 조회 및 난이도 락
     */
    @Operation(
            summary = "오늘의 퀴즈 조회",
            description = "난이도를 선택하여 오늘의 퀴즈를 조회, 최초 조회 시 난이도 락이 생성"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<QuizResponse>> fetchQuiz(
            @RequestParam String difficulty,
            @AuthenticationPrincipal UUID userId) {

        QuizResponse response = quizService.fetchQuiz(userId, difficulty);

        return ResponseEntity.ok(
                ApiResponse.success("퀴즈를 성공적으로 조회했습니다.", response)
        );
    }

    /**
     * 2-a. AI 채팅 SSE 스트림 — Kafka 응답 수신 대기
     *
     * <p>클라이언트는 {@code POST /chat}으로 Kafka 이벤트를 발행한 뒤,
     * 이 엔드포인트로 SSE 연결을 열어 AI 응답을 실시간 수신한다.</p>
     *
     * <p><b>인증 주의:</b> 브라우저 내장 {@code EventSource} API는 커스텀 헤더를 지원하지 않는다.
     * {@code Authorization} 헤더가 필요한 이 엔드포인트는 {@code fetch()} + 수동 SSE 파싱 방식으로
     * 호출해야 한다 (Option A). {@code EventSource} 사용이 필요하다면 팀 내 별도 협의가 필요하다.</p>
     */
    @Operation(
            summary = "AI 채팅 SSE 스트림",
            description = "AI 힌트 응답을 실시간으로 수신하는 SSE 연결을 수립한다. POST /chat 이후 호출."
    )
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatResponse(@AuthenticationPrincipal UUID userId) {
        return sseConnectionManager.connect(userId);
    }

    /**
     * 2-b. AI 챗봇 힌트 요청 — Kafka 이벤트 발행
     */
    @Operation(
            summary = "AI 챗봇 힌트",
            description = "CHAT_REQUEST 이벤트를 Kafka에 발행한다. AI 처리는 FastAPI 컨슈머가 비동기로 수행한다."
    )
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Void>> chatWithAi(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UUID userId) {

        quizService.chatWithAi(userId, request);

        return ResponseEntity.accepted()
                .body(ApiResponse.success("AI 힌트 요청이 접수되었습니다.", null));
    }

    /**
     * 3. 정답 제출 및 젤링 보상
     */
    @Operation(
            summary = "정답 제출",
            description = "선택한 답을 제출. 정답 시 랜덤 젤링(1~20)이 지급. 중복 제출은 허용되지 않음."
    )
    @PostMapping("/answer")
    public ResponseEntity<ApiResponse<AnswerResponse>> submitAnswer(
            @Valid @RequestBody AnswerRequest request,
            @AuthenticationPrincipal UUID userId) {

        AnswerResponse response = quizService.submitAnswer(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("정답이 성공적으로 제출되었습니다.", response)
        );
    }
}
