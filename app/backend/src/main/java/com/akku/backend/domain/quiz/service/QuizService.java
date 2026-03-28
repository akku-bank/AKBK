package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.quiz.dto.AnswerRequest;
import com.akku.backend.domain.quiz.dto.AnswerResponse;
import com.akku.backend.domain.quiz.dto.ChatRequest;
import com.akku.backend.domain.quiz.dto.ChatResponse;
import com.akku.backend.domain.quiz.dto.QuizResponse;
import com.akku.backend.domain.quiz.entity.ChatLog;
import com.akku.backend.domain.quiz.entity.Quiz;
import com.akku.backend.domain.quiz.entity.UserQuiz;
import com.akku.backend.domain.quiz.event.QuizChatEvent;
import com.akku.backend.domain.quiz.exception.QuizErrorCode;
import com.akku.backend.domain.quiz.repository.ChatLogRepository;
import com.akku.backend.domain.quiz.repository.QuizRepository;
import com.akku.backend.domain.quiz.repository.UserQuizRepository;
import com.akku.backend.domain.quiz.sse.SseConnectionManager;
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

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserQuizRepository userQuizRepository;
    private final ChatLogRepository chatLogRepository;
    private final UserRepository userRepository;
    private final JellingRepository jellingRepository;
    private final JellingTransactionRepository jellingTransactionRepository;
    private final QuizAiClient quizAiClient;
    private final SseConnectionManager sseConnectionManager;
    private final Clock clock;

    @Transactional
    public QuizResponse fetchQuiz(UUID userId, String difficulty) {
        LocalDateTime quizFrom = LocalDateTime.of(LocalDate.now(clock).minusDays(1), LocalTime.of(22, 0));
        LocalDateTime quizTo = LocalDateTime.of(LocalDate.now(clock), LocalTime.MIDNIGHT);

        Quiz quiz = quizRepository.findTopByDifficultyAndCreatedAtBetween(difficulty, quizFrom, quizTo)
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        LocalDateTime lockFrom = LocalDateTime.of(LocalDate.now(clock), LocalTime.MIDNIGHT);
        LocalDateTime lockTo = LocalDateTime.now(clock);

        UserQuiz userQuiz = userQuizRepository
                .findTopByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, lockFrom, lockTo)
                .map(existing -> {
                    if (!existing.getQuizId().equals(quiz.getId()) && existing.getRemainingCredits() < 100) {
                        throw new ApiException(QuizErrorCode.DIFFICULTY_CHANGE_NOT_ALLOWED);
                    }
                    if (existing.getQuizId().equals(quiz.getId())) {
                        return existing;
                    }
                    return userQuizRepository.save(UserQuiz.builder()
                            .userId(userId)
                            .quizId(quiz.getId())
                            .build());
                })
                .orElseGet(() -> userQuizRepository.save(UserQuiz.builder()
                        .userId(userId)
                        .quizId(quiz.getId())
                        .build()));

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
                todayChatJson,
                userQuiz.isSubmitted(),
                userQuiz.getIsCorrect()
        );
    }

    @Transactional
    public void chatWithAi(UUID userId, ChatRequest request) {
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(userId, request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        if (userQuiz.isSubmitted()) {
            throw new ApiException(QuizErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        String currentChatJson = chatLogRepository.findByUserIdAndQuizId(userId, request.quizId())
                .map(ChatLog::getChatJson)
                .orElse(null);

        QuizChatEvent event = QuizChatEvent.of(
                userId,
                request.quizId(),
                request.message(),
                userQuiz.getRemainingCredits(),
                quiz.getDifficulty(),
                user.getBirthDate()
        );

        ChatResponse response = quizAiClient.requestHint(event, currentChatJson);
        userQuiz.deductCredits(response.deductedCredits());
        upsertChatLog(userId, request.quizId(), response.chatJson());
        sseConnectionManager.send(userId, response, "chat-response");
        sseConnectionManager.complete(userId);
    }

    @Transactional
    public AnswerResponse submitAnswer(UUID userId, AnswerRequest request) {
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizIdForUpdate(userId, request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        if (userQuiz.isSubmitted()) {
            throw new ApiException(QuizErrorCode.QUIZ_ALREADY_SUBMITTED);
        }

        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ApiException(QuizErrorCode.QUIZ_NOT_FOUND));

        boolean isCorrect = quiz.getCorrectAnswer() == request.selectedAnswer();
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
            addQuizRewardJelling(userId, reward);
        }

        return new AnswerResponse(isCorrect, jellingReward);
    }

    @Transactional
    public void upsertChatLog(UUID userId, UUID quizId, String chatJson) {
        chatLogRepository.upsertChatJson(userId, quizId, chatJson);
    }

    private void addQuizRewardJelling(UUID userId, long rewardAmount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        Jelling jelling = jellingRepository.findById(userId)
                .orElseGet(() -> jellingRepository.save(Jelling.builder()
                        .user(user)
                        .balance(0L)
                        .build()));

        jelling.addBalance(rewardAmount);
        jellingRepository.save(jelling);

        jellingTransactionRepository.save(JellingTransaction.builder()
                .user(user)
                .amount(rewardAmount)
                .type("QUIZ_REWARD")
                .description("금융 퀴즈 정답 보상")
                .build());
    }
}
