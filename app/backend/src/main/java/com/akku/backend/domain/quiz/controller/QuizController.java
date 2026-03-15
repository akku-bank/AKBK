package com.akku.backend.domain.quiz.controller;

import com.akku.backend.domain.quiz.dto.AnswerRequest;
import com.akku.backend.domain.quiz.dto.AnswerResponse;
import com.akku.backend.domain.quiz.dto.ChatRequest;
import com.akku.backend.domain.quiz.dto.ChatResponse;
import com.akku.backend.domain.quiz.dto.QuizResponse;
import com.akku.backend.domain.quiz.service.QuizService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            @RequestAttribute("userId") UUID userId) {

        QuizResponse response = quizService.fetchQuiz(userId, difficulty);

        return ResponseEntity.ok(
                ApiResponse.success("퀴즈를 성공적으로 조회했습니다.", response)
        );
    }

    /**
     * 2. AI 챗봇 힌트 요청 (FastAPI 프록시)
     */
    @Operation(
            summary = "AI 챗봇 힌트",
            description = "AI 서버에 힌트를 요청하고 응답 채팅 로그를 DB에 저장. 크레딧 차감은 AI 서버에서 처리."
    )
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chatWithAi(
            @RequestBody ChatRequest request,
            @RequestAttribute("userId") UUID userId) {

        ChatResponse response = quizService.chatWithAi(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("AI 힌트를 성공적으로 받아왔습니다.", response)
        );
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
            @RequestBody AnswerRequest request,
            @RequestAttribute("userId") UUID userId) {

        AnswerResponse response = quizService.submitAnswer(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("정답이 성공적으로 제출되었습니다.", response)
        );
    }
}
