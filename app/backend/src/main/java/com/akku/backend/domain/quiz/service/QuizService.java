package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.quiz.client.QuizAiClient;
import com.akku.backend.domain.quiz.dto.AnswerRequest;
import com.akku.backend.domain.quiz.dto.AnswerResponse;
import com.akku.backend.domain.quiz.dto.ChatRequest;
import com.akku.backend.domain.quiz.dto.ChatResponse;
import com.akku.backend.domain.quiz.dto.QuizResponse;
import com.akku.backend.domain.quiz.entity.ChatLog;
import com.akku.backend.domain.quiz.entity.Quiz;
import com.akku.backend.domain.quiz.entity.UserQuiz;
import com.akku.backend.domain.quiz.exception.QuizErrorCode;
import com.akku.backend.domain.quiz.repository.ChatLogRepository;
import com.akku.backend.domain.quiz.repository.QuizRepository;
import com.akku.backend.domain.quiz.repository.UserQuizRepository;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 금융 퀴즈 챌린지 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserQuizRepository userQuizRepository;
    private final ChatLogRepository chatLogRepository;
    private final QuizAiClient quizAiClient;

    // ────────────────────────────────────────────────────────────────────────
    // 1. 퀴즈 조회 및 난이도 락 (GET /api/challenges/quizzes)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 금일 퀴즈 조회 후 난이도 락 생성
     *
     * <p>퀴즈 생성 시각 범위: 어제 22:00 ~ 오늘 00:00</p>
     * <p>Fail-Fast: 이미 AI 서버가 크레딧을 차감한 경우(remainingCredits < 100)
     * 다른 난이도로 재조회를 시도하면 {@link QuizErrorCode#DIFFICULTY_CHANGE_NOT_ALLOWED} 예외 발생.</p>
     *
     * @param userId     요청 자녀 ID (JWT에서 추출)
     * @param difficulty 요청 난이도 (HIGH / MEDIUM / LOW)
     * @return 퀴즈 정보 및 잔여 크레딧
     */
    @Transactional
    public QuizResponse fetchQuiz(UUID userId, String difficulty) {
        // 1. 오늘의 퀴즈 조회 (어제 22:00 ~ 오늘 00:00)
        LocalDateTime quizFrom = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(22, 0));
        LocalDateTime quizTo   = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);

        Quiz quiz = quizRepository.findTopByDifficultyAndCreatedAtBetween(difficulty, quizFrom, quizTo)
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        // 2. 기존 UserQuiz 레코드 확인 (난이도 락 검증)
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(userId, quiz.getId())
                .map(existing -> {
                    // AI 서버가 이미 크레딧을 차감했다면 난이도 변경 불가
                    if (existing.getRemainingCredits() < 100) {
                        throw new ApiException(QuizErrorCode.DIFFICULTY_CHANGE_NOT_ALLOWED);
                    }
                    // 크레딧이 아직 100이면 단순 재조회 — 새로 생성 불필요
                    return existing;
                })
                .orElseGet(() -> {
                    // 최초 조회: 난이도 락 레코드 INSERT (remainingCredits 기본값 100)
                    UserQuiz newLock = UserQuiz.builder()
                            .userId(userId)
                            .quizId(quiz.getId())
                            .build();
                    return userQuizRepository.save(newLock);
                });

        return new QuizResponse(
                quiz.getId(),
                quiz.getTopic(),
                quiz.getDifficulty(),
                quiz.getProblemJson(),
                quiz.getExplanation(),
                userQuiz.getRemainingCredits()
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // 2. AI 챗봇 힌트 프록시 (POST /api/challenges/quizzes/chat)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * FastAPI AI 서버로 힌트 요청을 프록시하고, 응답 채팅 로그를 DB에 저장.
     *
     * <p><b>트랜잭션 분리 전략</b>: 이 메서드에는 {@code @Transactional}을 걸지 않는다.
     * RestClient 최대 응답 대기 시간이 15초이므로, 전체를 하나의 트랜잭션으로 감쌀 경우
     * DB 커넥션 풀이 고갈될 수 있다. 각 repository 호출은 Spring Data JPA가 자체적으로
     * 독립 트랜잭션을 열고 닫으므로, 별도 어노테이션 없이도 정합성이 보장된다.</p>
     *
     * @param userId  요청 자녀 ID
     * @param request 힌트 요청 (quizId + 사용자 메시지)
     * @return AI 응답
     */
    public ChatResponse chatWithAi(UUID userId, ChatRequest request) {
        // 1. Chat Freeze: 이미 제출한 퀴즈는 AI 힌트 요청 불가 (자체 읽기 TX via repository)
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(userId, request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        if (userQuiz.isSubmitted()) {
            throw new ApiException(QuizErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        // 2. FastAPI 서버로 힌트 프록시 요청 — DB 트랜잭션 없음
        ChatResponse aiResponse = quizAiClient.requestHint(request);

        // 3. chatJson 구성: 사용자 입력 + AI 응답 모두 포함 (자체 쓰기 TX via repository)
        String chatJsonToSave = buildChatJson(request.message(), aiResponse);
        saveChatLog(userId, request.quizId(), chatJsonToSave);

        return aiResponse;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3. 정답 제출 및 젤링 보상 (POST /api/challenges/quizzes/answer)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 정답을 제출하고 결과에 따라 젤링 보상 금액을 산출한다.
     *
     * <p>Idempotency: isSubmitted=true 이면 {@link QuizErrorCode#QUIZ_ALREADY_SUBMITTED} 예외.</p>
     * <p>보상 금액만 계산하고 실제 Jelling DB 처리는 Jelling 도메인에 위임한다.</p>
     *
     * @param userId  요청 자녀 ID
     * @param request 정답 제출 (quizId + selectedAnswer)
     * @return 정답 여부 및 지급 예정 젤링 양
     */
    @Transactional
    public AnswerResponse submitAnswer(UUID userId, AnswerRequest request) {
        // 1. Idempotency 검증 — 중복 제출 방지 (Fail-Fast)
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(userId, request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        if (userQuiz.isSubmitted()) {
            throw new ApiException(QuizErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        // 2. 퀴즈 정답 조회 및 채점
        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        boolean isCorrect = (quiz.getCorrectAnswer() == request.selectedAnswer());

        // 3. UserQuiz 상태 업데이트 (JPA Dirty Checking 자동 반영)
        userQuiz.submit(isCorrect);

        // 4. 정답 시 젤링 보상 — DB 직접 조작 없이 보상 금액만 산출
        Long jellingReward = null;
        if (isCorrect) {
            long reward = ThreadLocalRandom.current().nextLong(1, 21); // 1 이상 20 이하
            // TODO: Call Jelling domain to add reward (amount: reward)
            jellingReward = reward;
        }

        return new AnswerResponse(isCorrect, jellingReward);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * FastAPI 응답의 chatJson이 있으면 그대로 사용(전체 대화 기록 포함),
     * 없으면 사용자 메시지 + AI 응답으로 최소 JSON을 직접 구성한다.
     *
     * <p>chat_json은 반드시 사용자 입력과 AI 응답을 모두 포함해야 한다.</p>
     */
    private String buildChatJson(String userMessage, ChatResponse aiResponse) {
        if (aiResponse.chatJson() != null && !aiResponse.chatJson().isBlank()) {
            return aiResponse.chatJson();
        }
        // FastAPI chatJson 누락 시: 사용자 메시지 + AI 응답을 최소 JSON으로 보존
        String safeUser  = userMessage == null ? "" : userMessage.replace("\"", "\\\"");
        String safeReply = aiResponse.reply() == null ? "" : aiResponse.reply().replace("\"", "\\\"");
        return String.format(
                "{\"messages\":[{\"role\":\"user\",\"content\":\"%s\"},{\"role\":\"assistant\",\"content\":\"%s\"}]}",
                safeUser, safeReply);
    }

    /**
     * 채팅 로그 UPSERT.
     *
     * <p>find → update 또는 save 흐름에서 각 repository 호출이 독립 트랜잭션으로 실행된다.
     * update 경로에서는 detached 엔티티를 명시적으로 {@code save()} 하여 병합한다.</p>
     */
    private void saveChatLog(UUID userId, UUID quizId, String chatJson) {
        chatLogRepository.findByUserIdAndQuizId(userId, quizId)
                .ifPresentOrElse(
                        log -> {
                            log.updateChatJson(chatJson);
                            chatLogRepository.save(log); // detached entity → merge
                        },
                        () -> chatLogRepository.save(
                                ChatLog.builder()
                                        .userId(userId)
                                        .quizId(quizId)
                                        .chatJson(chatJson)
                                        .build()
                        )
                );
    }
}
