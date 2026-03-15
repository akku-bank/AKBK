package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.quiz.dto.AnswerRequest;
import com.akku.backend.domain.quiz.dto.AnswerResponse;
import com.akku.backend.domain.quiz.dto.ChatRequest;
import com.akku.backend.domain.quiz.dto.ChatResponse;
import com.akku.backend.domain.quiz.dto.QuizResponse;
import com.akku.backend.domain.quiz.entity.ChatLog;
import com.akku.backend.domain.quiz.entity.Jelling;
import com.akku.backend.domain.quiz.entity.JellingTransaction;
import com.akku.backend.domain.quiz.entity.Quiz;
import com.akku.backend.domain.quiz.entity.UserQuiz;
import com.akku.backend.domain.quiz.exception.QuizErrorCode;
import com.akku.backend.domain.quiz.repository.ChatLogRepository;
import com.akku.backend.domain.quiz.repository.JellingRepository;
import com.akku.backend.domain.quiz.repository.JellingTransactionRepository;
import com.akku.backend.domain.quiz.repository.QuizRepository;
import com.akku.backend.domain.quiz.repository.UserQuizRepository;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

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
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserQuizRepository userQuizRepository;
    private final ChatLogRepository chatLogRepository;
    private final JellingRepository jellingRepository;
    private final JellingTransactionRepository jellingTransactionRepository;

    @Qualifier("fastApiClient")
    private final RestClient fastApiClient;

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
                    // 최초 조회: 난이도 락 레코드 INSERT
                    UserQuiz newLock = UserQuiz.builder()
                            .userId(userId)
                            .quizId(quiz.getId())
                            .remainingCredits(100)
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
     * FastAPI AI 서버로 힌트 요청을 프록시하고, 응답 채팅 로그를 DB에 저장
     *
     * <p>크레딧 차감은 AI 서버에서 직접 처리 — 백엔드 미개입.</p>
     *
     * @param userId  요청 자녀 ID
     * @param request 힌트 요청 (quizId + 사용자 메시지)
     * @return AI 응답
     */
    @Transactional
    public ChatResponse chatWithAi(UUID userId, ChatRequest request) {
        // 1. FastAPI 서버로 힌트 프록시 요청
        ChatResponse aiResponse;
        try {
            aiResponse = fastApiClient
                    .post()
                    .uri("/quiz/chat")
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (Exception e) {
            throw new ApiException(QuizErrorCode.AI_SERVER_ERROR);
        }

        if (aiResponse == null) {
            throw new ApiException(QuizErrorCode.AI_SERVER_ERROR);
        }

        // 2. 채팅 로그 UPSERT (존재 시 갱신, 없으면 신규 저장)
        String chatJsonToSave = aiResponse.chatJson() != null ? aiResponse.chatJson() : "{}";

        chatLogRepository.findByUserIdAndQuizId(userId, request.quizId())
                .ifPresentOrElse(
                        log -> log.updateChatJson(chatJsonToSave),
                        () -> chatLogRepository.save(
                                ChatLog.builder()
                                        .userId(userId)
                                        .quizId(request.quizId())
                                        .chatJson(chatJsonToSave)
                                        .build()
                        )
                );

        return aiResponse;
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3. 정답 제출 및 젤링 보상 (POST /api/challenges/quizzes/answer)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 정답을 제출하고 결과에 따라 젤링 보상을 지급
     *
     * <p>Idempotency: isSubmitted=true 이면 {@link QuizErrorCode#QUIZ_ALREADY_SUBMITTED} 예외.</p>
     * <p>보상 지급: 정답 시 1~20 사이 랜덤 젤링 적립, Jelling 지갑 미존재 시 자동 생성(orElseGet).</p>
     *
     * @param userId  요청 자녀 ID
     * @param request 정답 제출 (quizId + selectedAnswer)
     * @return 정답 여부 및 지급 젤링 양
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

        // 4. 정답 시 젤링 보상 지급 (단일 트랜잭션 내 처리)
        Long jellingReward = null;
        if (isCorrect) {
            long reward = ThreadLocalRandom.current().nextLong(1, 21); // 1 이상 20 이하

            // 4-A. Jelling 지갑 조회 — 없으면 방어적 생성 (orElseGet)
            Jelling jelling = jellingRepository.findById(userId)
                    .orElseGet(() -> jellingRepository.save(
                            Jelling.builder()
                                    .userId(userId)
                                    .balance(0L)
                                    .build()
                    ));

            // 4-B. 잔액 적립
            jelling.addBalance(reward);

            // 4-C. 젤링 변동 내역 INSERT
            jellingTransactionRepository.save(
                    JellingTransaction.builder()
                            .userId(userId)
                            .amount(reward)
                            .type("EARNED")
                            .description("퀴즈 정답 적립")
                            .build()
            );

            jellingReward = reward;
        }

        return new AnswerResponse(isCorrect, jellingReward);
    }
}
