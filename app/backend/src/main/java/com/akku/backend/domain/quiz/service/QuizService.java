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
    private final QuizAiClient quizAiClient; // RestClient를 감싼 래퍼 클래스

    // ────────────────────────────────────────────────────────────────────────
    // 1. 퀴즈 조회 및 난이도 락 (GET /api/challenges/quizzes)
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public QuizResponse fetchQuiz(UUID userId, String difficulty) {
        LocalDateTime quizFrom = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(22, 0));
        LocalDateTime quizTo   = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);

        Quiz quiz = quizRepository.findTopByDifficultyAndCreatedAtBetween(difficulty, quizFrom, quizTo)
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(userId, quiz.getId())
                .map(existing -> {
                    if (existing.getRemainingCredits() < 100) {
                        throw new ApiException(QuizErrorCode.DIFFICULTY_CHANGE_NOT_ALLOWED);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    UserQuiz newLock = UserQuiz.builder()
                            .userId(userId)
                            .quizId(quiz.getId())
                            .build(); // @Builder.Default로 remainingCredits=100 설정됨
                    return userQuizRepository.save(newLock);
                });

        // 당일 채팅 로그만 조회
        String todayChatJson = chatLogRepository.findByUserIdAndQuizId(userId, quiz.getId())
                .filter(log -> log.getCreatedAt().equals(LocalDate.now()))
                .map(ChatLog::getChatJson)
                .orElse(null);

        return new QuizResponse(
                quiz.getId(),
                quiz.getTopic(),
                quiz.getDifficulty(),
                quiz.getProblemJson(),
                quiz.getExplanation(),
                userQuiz.getRemainingCredits(),
                todayChatJson
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // 2. AI 챗봇 힌트 프록시 (POST /api/challenges/quizzes/chat)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * @Transactional이 없음에 주의! 외부 통신 시 DB 커넥션 풀 고갈을 막기 위함.
     */
    public ChatResponse chatWithAi(UUID userId, ChatRequest request) {
        // 1. DB 읽기 (자체 트랜잭션) - Chat Freeze 검증
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(userId, request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        if (userQuiz.isSubmitted()) {
            throw new ApiException(QuizErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        // 2. 외부 API 통신 (트랜잭션 없음)
        ChatResponse aiResponse = quizAiClient.requestHint(request);

        // 3. DB 쓰기 (자체 트랜잭션) - 사용자 메시지와 AI 응답 모두 포함하여 저장
        String chatJsonToSave = buildChatJson(request.message(), aiResponse);
        saveChatLog(userId, request.quizId(), chatJsonToSave);

        return aiResponse;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3. 정답 제출 및 젤링 보상 (POST /api/challenges/quizzes/answer)
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public AnswerResponse submitAnswer(UUID userId, AnswerRequest request) {
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(userId, request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        if (userQuiz.isSubmitted()) {
            throw new ApiException(QuizErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        boolean isCorrect = (quiz.getCorrectAnswer() == request.selectedAnswer());
        userQuiz.submit(isCorrect);

        Long jellingReward = null;
        if (isCorrect) {
            long reward = ThreadLocalRandom.current().nextLong(1, 21); // 1~20 랜덤 보상
            // TODO: Call Jelling domain to add reward (amount: reward)
            // 예시: jellingService.addReward(userId, reward);
            jellingReward = reward;
        }

        return new AnswerResponse(isCorrect, jellingReward);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────

    private String buildChatJson(String userMessage, ChatResponse aiResponse) {
        if (aiResponse.chatJson() != null && !aiResponse.chatJson().isBlank()) {
            return aiResponse.chatJson();
        }
        String safeUser  = userMessage == null ? "" : userMessage.replace("\"", "\\\"");
        String safeReply = aiResponse.reply() == null ? "" : aiResponse.reply().replace("\"", "\\\"");
        return String.format(
                "{\"messages\":[{\"role\":\"user\",\"content\":\"%s\"},{\"role\":\"assistant\",\"content\":\"%s\"}]}",
                safeUser, safeReply);
    }

    private void saveChatLog(UUID userId, UUID quizId, String chatJson) {
        chatLogRepository.findByUserIdAndQuizId(userId, quizId)
                .ifPresentOrElse(
                        log -> {
                            log.updateChatJson(chatJson);
                            chatLogRepository.save(log);
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