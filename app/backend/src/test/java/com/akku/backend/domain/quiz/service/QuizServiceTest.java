package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.quiz.client.QuizAiClient;
import com.akku.backend.domain.quiz.dto.*;
import com.akku.backend.domain.quiz.entity.*;
import com.akku.backend.domain.quiz.exception.QuizErrorCode;
import com.akku.backend.domain.quiz.repository.*;
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @InjectMocks
    private QuizService quizService;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private UserQuizRepository userQuizRepository;

    @Mock
    private ChatLogRepository chatLogRepository;

    @Mock
    private QuizAiClient quizAiClient;

    @Nested
    @DisplayName("1. 퀴즈 조회 및 난이도 락 (fetchQuiz)")
    class FetchQuizTests {

        @Test
        @DisplayName("성공 - 최초 조회 시 난이도 락 생성 및 remainingCredits 기본값 100 확인")
        void fetchQuiz_Success_Initial() {
            // given
            UUID userId = UUID.randomUUID();
            String difficulty = "HIGH";
            Quiz mockQuiz = mock(Quiz.class);
            given(mockQuiz.getId()).willReturn(UUID.randomUUID());
            given(quizRepository.findTopByDifficultyAndCreatedAtBetween(eq(difficulty), any(), any()))
                    .willReturn(Optional.of(mockQuiz));
            given(userQuizRepository.findByUserIdAndQuizId(any(), any())).willReturn(Optional.empty());
            given(chatLogRepository.findByUserIdAndQuizId(any(), any())).willReturn(Optional.empty());

            UserQuiz savedUserQuiz = UserQuiz.builder().userId(userId).quizId(mockQuiz.getId()).build();
            // 강제로 100을 넣지 않아도 엔티티 내부 @Builder.Default 덕분에 100이 유지되어야 함
            given(userQuizRepository.save(any(UserQuiz.class))).willReturn(savedUserQuiz);

            // when
            QuizResponse response = quizService.fetchQuiz(userId, difficulty);

            // then
            assertNotNull(response);
            assertEquals(100, response.remainingCredits());
            assertNull(response.chatJson());
            verify(userQuizRepository).save(any(UserQuiz.class));
        }

        @Test
        @DisplayName("실패 - 이미 크레딧이 차감된 경우 난이도 변경 불가")
        void fetchQuiz_Fail_DifficultyLock() {
            // given
            UUID userId = UUID.randomUUID();
            String difficulty = "LOW";
            Quiz mockQuiz = mock(Quiz.class);
            given(mockQuiz.getId()).willReturn(UUID.randomUUID());
            given(quizRepository.findTopByDifficultyAndCreatedAtBetween(eq(difficulty), any(), any()))
                    .willReturn(Optional.of(mockQuiz));

            // 크레딧이 90인 상태 (이미 힌트를 씀)
            UserQuiz existingUserQuiz = mock(UserQuiz.class);
            given(existingUserQuiz.getRemainingCredits()).willReturn(90);
            given(userQuizRepository.findByUserIdAndQuizId(any(), any())).willReturn(Optional.of(existingUserQuiz));

            // when & then
            ApiException ex = assertThrows(ApiException.class, () -> quizService.fetchQuiz(userId, difficulty));
            assertEquals(QuizErrorCode.DIFFICULTY_CHANGE_NOT_ALLOWED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("2. AI 챗봇 힌트 (chatWithAi)")
    class ChatWithAiTests {

        @Test
        @DisplayName("성공 - AI 응답 수신 시 사용자 메시지와 AI 응답이 채팅 로그에 저장됨")
        void chatWithAi_Success_SavesBothUserAndAiMessage() {
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            String userMessage = "힌트 주세요";
            ChatRequest request = new ChatRequest(quizId, userMessage);

            // ── Scenario A: FastAPI가 chatJson을 직접 반환 ──────────────────────
            String prebuiltChatJson = "{\"messages\":[{\"role\":\"user\",\"content\":\"힌트 주세요\"}]}";
            given(userQuizRepository.findByUserIdAndQuizId(userId, quizId))
                    .willReturn(Optional.of(UserQuiz.builder().userId(userId).quizId(quizId).build()));
            given(quizAiClient.requestHint(request))
                    .willReturn(new ChatResponse("AI answer", prebuiltChatJson));
            given(chatLogRepository.findByUserIdAndQuizId(userId, quizId))
                    .willReturn(Optional.empty());

            quizService.chatWithAi(userId, request);

            ArgumentCaptor<ChatLog> captorA = ArgumentCaptor.forClass(ChatLog.class);
            verify(chatLogRepository).save(captorA.capture());
            assertEquals(prebuiltChatJson, captorA.getValue().getChatJson());

            // ── Scenario B: FastAPI가 chatJson=null 반환 → 서비스 레이어에서 조립 ──
            reset(userQuizRepository, quizAiClient, chatLogRepository);
            given(userQuizRepository.findByUserIdAndQuizId(userId, quizId))
                    .willReturn(Optional.of(UserQuiz.builder().userId(userId).quizId(quizId).build()));
            given(quizAiClient.requestHint(request))
                    .willReturn(new ChatResponse("AI answer", null));
            given(chatLogRepository.findByUserIdAndQuizId(userId, quizId))
                    .willReturn(Optional.empty());

            quizService.chatWithAi(userId, request);

            ArgumentCaptor<ChatLog> captorB = ArgumentCaptor.forClass(ChatLog.class);
            verify(chatLogRepository).save(captorB.capture());
            String builtJson = captorB.getValue().getChatJson();
            assertNotNull(builtJson);
            assertTrue(builtJson.contains(userMessage));
            assertTrue(builtJson.contains("AI answer"));
        }

        @Test
        @DisplayName("실패 - 이미 제출된 퀴즈에는 AI 힌트 요청 불가 (Freeze)")
        void chatWithAi_Fail_AlreadySubmitted() {
            // given
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            ChatRequest request = new ChatRequest(quizId, "힌트 주세요");

            UserQuiz submittedQuiz = UserQuiz.builder().userId(userId).quizId(quizId).build();
            submittedQuiz.submit(true); // 이미 제출된 상태로 세팅
            given(userQuizRepository.findByUserIdAndQuizId(userId, quizId))
                    .willReturn(Optional.of(submittedQuiz));

            // when & then
            ApiException ex = assertThrows(ApiException.class, () -> quizService.chatWithAi(userId, request));
            assertEquals(QuizErrorCode.QUIZ_ALREADY_SUBMITTED, ex.getErrorCode());

            // AI 서버(QuizAiClient)가 호출되지 않았는지 검증
            verifyNoInteractions(quizAiClient);
        }
    }

    @Nested
    @DisplayName("3. 정답 제출 및 보상 (submitAnswer)")
    class SubmitAnswerTests {

        @Test
        @DisplayName("성공 - 정답 시 1~20 사이 랜덤 보상 지급 (젤링 TODO 처리)")
        void submitAnswer_Correct_Reward() {
            // given
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            AnswerRequest request = new AnswerRequest(quizId, 1);

            UserQuiz userQuiz = UserQuiz.builder().userId(userId).quizId(quizId).build();
            given(userQuizRepository.findByUserIdAndQuizIdForUpdate(userId, quizId)).willReturn(Optional.of(userQuiz));

            Quiz quiz = mock(Quiz.class);
            given(quiz.getCorrectAnswer()).willReturn(1); // 1번이 정답
            given(quizRepository.findById(quizId)).willReturn(Optional.of(quiz));

            // when
            AnswerResponse response = quizService.submitAnswer(userId, request);

            // then
            assertTrue(response.isCorrect());
            assertNotNull(response.jellingReward());
            assertTrue(response.jellingReward() >= 1 && response.jellingReward() <= 20);
        }
    }
}