package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.jelling.entity.Jelling;
import com.akku.backend.domain.jelling.entity.JellingTransaction;
import com.akku.backend.domain.jelling.repository.JellingRepository;
import com.akku.backend.domain.jelling.repository.JellingTransactionRepository;
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

import java.time.Clock;
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
    private final JellingRepository jellingRepository;
    private final JellingTransactionRepository jellingTransactionRepository;
    private final QuizKafkaProducer quizKafkaProducer;
    private final Clock clock;

    // ────────────────────────────────────────────────────────────────────────
    // 1. 퀴즈 조회 및 난이도 락 (GET /api/challenges/quizzes)
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public QuizResponse fetchQuiz(UUID userId, String difficulty) {
        LocalDateTime quizFrom = LocalDateTime.of(LocalDate.now(clock).minusDays(1), LocalTime.of(22, 0));
        LocalDateTime quizTo   = LocalDateTime.of(LocalDate.now(clock), LocalTime.MIDNIGHT);

        Quiz quiz = quizRepository.findTopByDifficultyAndCreatedAtBetween(difficulty, quizFrom, quizTo)
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        LocalDateTime lockFrom = LocalDateTime.of(LocalDate.now(clock), LocalTime.MIDNIGHT);
        LocalDateTime lockTo   = LocalDateTime.now(clock);

        UserQuiz userQuiz = userQuizRepository
                .findTopByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, lockFrom, lockTo)
                .map(existing -> {
                    // 다른 난이도(퀴즈)에서 이미 힌트를 사용했으면 변경 불가
                    if (!existing.getQuizId().equals(quiz.getId()) && existing.getRemainingCredits() < 100) {
                        throw new ApiException(QuizErrorCode.DIFFICULTY_CHANGE_NOT_ALLOWED);
                    }
                    // 같은 퀴즈면 기존 락 반환
                    if (existing.getQuizId().equals(quiz.getId())) {
                        return existing;
                    }
                    // 다른 퀴즈지만 힌트 미사용(credits=100) — 새 난이도로 락 생성
                    return userQuizRepository.save(UserQuiz.builder()
                            .userId(userId)
                            .quizId(quiz.getId())
                            .build());
                })
                .orElseGet(() -> userQuizRepository.save(UserQuiz.builder()
                        .userId(userId)
                        .quizId(quiz.getId())
                        .build()));

        // 당일 채팅 로그만 조회
        String todayChatJson = chatLogRepository.findByUserIdAndQuizId(userId, quiz.getId())
                .filter(log -> log.getCreatedAt().equals(LocalDate.now(clock)))
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
        userQuiz.submit(isCorrect, LocalDate.now(clock));

        Long jellingReward = null;
        if (isCorrect) {
            long reward = ThreadLocalRandom.current().nextLong(1, 21); // 1~20 랜덤 보상
            Jelling jelling = jellingRepository.findByUserIdWithLock(userId)
                    .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));
            jelling.addBalance(reward);
            jellingTransactionRepository.save(JellingTransaction.builder()
                    .user(jelling.getUser())
                    .amount(reward)
                    .type("QUIZ_REWARD")
                    .description("퀴즈 정답 보상")
                    .build());
            jellingReward = reward;
        }

        return new AnswerResponse(isCorrect, jellingReward);
    }

    // ────────────────────────────────────────────────────────────────────────
    // 4. 채팅 로그 UPSERT — Kafka 컨슈머 전용 (CHAT_RESPONSE 수신 시 호출)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * chat_logs 테이블에 대해 UPSERT를 수행한다.
     * {@link com.akku.backend.domain.quiz.kafka.QuizKafkaConsumer}가 CHAT_RESPONSE를
     * 수신한 직후 호출하며, 이 메서드의 트랜잭션이 커밋된 뒤에 SSE 푸시가 진행된다.
     *
     * <p>호출 전 컨슈머가 chatJson null/blank 여부를 반드시 검증해야 한다.
     * 이 메서드는 chatJson이 유효함을 전제로 동작한다.</p>
     */
    @Transactional
    public void upsertChatLog(UUID userId, UUID quizId, String chatJson) {
        chatLogRepository.upsertChatJson(userId, quizId, chatJson);
    }
}
