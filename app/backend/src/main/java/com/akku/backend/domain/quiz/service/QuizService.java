package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.quiz.dto.AnswerRequest;
import com.akku.backend.domain.quiz.dto.AnswerResponse;
import com.akku.backend.domain.quiz.dto.ChatRequest;
import com.akku.backend.domain.quiz.dto.QuizResponse;
import com.akku.backend.domain.quiz.entity.ChatLog;
import com.akku.backend.domain.quiz.entity.Quiz;
import com.akku.backend.domain.quiz.entity.UserQuiz;
import com.akku.backend.domain.quiz.event.QuizChatEvent;
import com.akku.backend.domain.quiz.exception.QuizErrorCode;
import com.akku.backend.domain.quiz.kafka.QuizKafkaProducer;
import com.akku.backend.domain.quiz.repository.ChatLogRepository;
import com.akku.backend.domain.quiz.repository.QuizRepository;
import com.akku.backend.domain.quiz.repository.UserQuizRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
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
    private final UserRepository userRepository;
    private final QuizKafkaProducer quizKafkaProducer;

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
    // 2. AI 챗봇 힌트 — Kafka 이벤트 발행 (POST /api/challenges/quizzes/chat)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 동기 RestClient 호출 대신 Kafka CHAT_REQUEST 이벤트를 발행한다.
     * AI 처리는 FastAPI 컨슈머가 비동기로 수행하며, 202 Accepted 로 즉시 응답한다.
     * 트랜잭션 없음 — Kafka 발행은 DB 트랜잭션과 분리되어야 한다.
     */
    public void chatWithAi(UUID userId, ChatRequest request) {
        // 1. Chat Freeze 검증
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(userId, request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        if (userQuiz.isSubmitted()) {
            throw new ApiException(QuizErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        // 2. 이벤트 조립에 필요한 데이터 조회
        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 3. Kafka CHAT_REQUEST 이벤트 발행
        QuizChatEvent event = QuizChatEvent.of(
                userId,
                request.quizId(),
                request.message(),
                userQuiz.getRemainingCredits(),
                quiz.getDifficulty(),
                user.getBirthDate()
        );
        quizKafkaProducer.publishChatRequest(event);

        // TODO: 채팅 로그는 FastAPI → Kafka CHAT_RESPONSE 수신 후 컨슈머에서 저장
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3. 정답 제출 및 젤링 보상 (POST /api/challenges/quizzes/answer)
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public AnswerResponse submitAnswer(UUID userId, AnswerRequest request) {
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizIdForUpdate(userId, request.quizId())
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
            jellingReward = reward;
        }

        return new AnswerResponse(isCorrect, jellingReward);
    }
}
