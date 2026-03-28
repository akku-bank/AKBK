package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.jelling.entity.Jelling;
import com.akku.backend.domain.jelling.repository.JellingRepository;
import com.akku.backend.domain.jelling.repository.JellingTransactionRepository;
import com.akku.backend.domain.quiz.dto.*;
import com.akku.backend.domain.quiz.entity.*;
import com.akku.backend.domain.quiz.client.QuizAiClient;
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
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

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
    private UserRepository userRepository;

    @Mock
    private JellingRepository jellingRepository;

    @Mock
    private JellingTransactionRepository jellingTransactionRepository;

    @Mock
    private QuizAiClient quizAiClient;

    @Mock
    private SseConnectionManager sseConnectionManager;

    @Mock
    private Clock clock;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-03-27T10:00:00Z"));
    }

    @Nested
    class FetchQuizTests {

        @Test
        void fetchQuiz_Success_Initial() {
            UUID userId = UUID.randomUUID();
            String difficulty = "HIGH";
            Quiz mockQuiz = mock(Quiz.class);
            given(mockQuiz.getId()).willReturn(UUID.randomUUID());
            given(quizRepository.findTopByDifficultyAndCreatedAtBetween(eq(difficulty), any(), any()))
                    .willReturn(Optional.of(mockQuiz));
            given(userQuizRepository.findTopByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(any(), any(), any()))
                    .willReturn(Optional.empty());
            given(chatLogRepository.findByUserIdAndQuizId(any(), any())).willReturn(Optional.empty());

            UserQuiz savedUserQuiz = UserQuiz.builder().userId(userId).quizId(mockQuiz.getId()).build();
            given(userQuizRepository.save(any(UserQuiz.class))).willReturn(savedUserQuiz);

            QuizResponse response = quizService.fetchQuiz(userId, difficulty);

            assertNotNull(response);
            assertEquals(100, response.remainingCredits());
            assertNull(response.chatJson());
            verify(userQuizRepository).save(any(UserQuiz.class));
        }

        @Test
        void fetchQuiz_Fail_DifficultyLock() {
            UUID userId = UUID.randomUUID();
            String difficulty = "LOW";
            Quiz mockQuiz = mock(Quiz.class);
            given(mockQuiz.getId()).willReturn(UUID.randomUUID());
            given(quizRepository.findTopByDifficultyAndCreatedAtBetween(eq(difficulty), any(), any()))
                    .willReturn(Optional.of(mockQuiz));

            UserQuiz existingUserQuiz = mock(UserQuiz.class);
            given(existingUserQuiz.getRemainingCredits()).willReturn(90);
            given(existingUserQuiz.getQuizId()).willReturn(UUID.randomUUID());
            given(userQuizRepository.findTopByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(any(), any(), any()))
                    .willReturn(Optional.of(existingUserQuiz));

            ApiException ex = assertThrows(ApiException.class, () -> quizService.fetchQuiz(userId, difficulty));
            assertEquals(QuizErrorCode.DIFFICULTY_CHANGE_NOT_ALLOWED, ex.getErrorCode());
        }
    }

    @Nested
    class ChatWithAiTests {

        @Test
        void chatWithAi_Success_RequestsFastApiAndSendsSse() {
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            String userMessage = "힌트 주세요";
            LocalDate birthDate = LocalDate.of(2015, 3, 10);
            ChatRequest request = new ChatRequest(quizId, userMessage);

            UserQuiz userQuiz = UserQuiz.builder().userId(userId).quizId(quizId).build();
            given(userQuizRepository.findByUserIdAndQuizId(userId, quizId)).willReturn(Optional.of(userQuiz));

            Quiz mockQuiz = mock(Quiz.class);
            given(mockQuiz.getDifficulty()).willReturn("HIGH");
            given(quizRepository.findById(quizId)).willReturn(Optional.of(mockQuiz));

            User mockUser = mock(User.class);
            given(mockUser.getBirthDate()).willReturn(birthDate);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(chatLogRepository.findByUserIdAndQuizId(userId, quizId)).willReturn(Optional.empty());
            given(quizAiClient.requestHint(any(QuizChatEvent.class), isNull()))
                    .willReturn(new ChatResponse("AI 힌트", 10, "[{\"role\":\"assistant\",\"content\":\"AI 힌트\"}]"));

            quizService.chatWithAi(userId, request);

            ArgumentCaptor<QuizChatEvent> eventCaptor = ArgumentCaptor.forClass(QuizChatEvent.class);
            verify(quizAiClient).requestHint(eventCaptor.capture(), isNull());
            QuizChatEvent sent = eventCaptor.getValue();

            assertNotNull(sent.eventId());
            assertEquals("CHAT_REQUEST", sent.eventType());
            assertEquals(userId, sent.userId());
            assertEquals(quizId, sent.quizId());
            assertEquals(userMessage, sent.message());
            assertEquals("HIGH", sent.difficulty());
            assertEquals(birthDate, sent.birthDate());
            assertEquals(100, sent.remainingCredits());
            assertEquals(90, userQuiz.getRemainingCredits());

            verify(chatLogRepository).upsertChatJson(userId, quizId, "[{\"role\":\"assistant\",\"content\":\"AI 힌트\"}]");
            verify(sseConnectionManager).send(eq(userId), any(ChatResponse.class), eq("chat-response"));
            verify(sseConnectionManager).complete(userId);
        }

        @Test
        void chatWithAi_Fail_AlreadySubmitted() {
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            ChatRequest request = new ChatRequest(quizId, "힌트 주세요");

            UserQuiz submittedQuiz = UserQuiz.builder().userId(userId).quizId(quizId).build();
            submittedQuiz.submit(true, LocalDate.now(clock));
            given(userQuizRepository.findByUserIdAndQuizId(userId, quizId))
                    .willReturn(Optional.of(submittedQuiz));

            ApiException ex = assertThrows(ApiException.class, () -> quizService.chatWithAi(userId, request));
            assertEquals(QuizErrorCode.QUIZ_ALREADY_SUBMITTED, ex.getErrorCode());

            verifyNoInteractions(quizAiClient, sseConnectionManager);
        }
    }

    @Nested
    class SubmitAnswerTests {

        @Test
        @DisplayName("성공 - 정답 시 1~20 사이 랜덤 보상 지급 (젤링 TODO 처리)")
        void submitAnswer_Correct_Reward() {
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            AnswerRequest request = new AnswerRequest(quizId, 1);

            UserQuiz userQuiz = UserQuiz.builder().userId(userId).quizId(quizId).build();
            given(userQuizRepository.findByUserIdAndQuizIdForUpdate(userId, quizId)).willReturn(Optional.of(userQuiz));

            Quiz quiz = mock(Quiz.class);
            given(quiz.getCorrectAnswer()).willReturn(1);
            given(quizRepository.findById(quizId)).willReturn(Optional.of(quiz));

            User mockUser = mock(User.class);
            Jelling jelling = spy(Jelling.builder().user(mockUser).build());
            given(jellingRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(jelling));

            User mockUser = mock(User.class);
            Jelling jelling = spy(Jelling.builder().user(mockUser).build());
            given(jellingRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(jelling));

            AnswerResponse response = quizService.submitAnswer(userId, request);

            assertTrue(response.isCorrect());
            assertNotNull(response.jellingReward());
            assertTrue(response.jellingReward() >= 1 && response.jellingReward() <= 20);
            verify(jelling).addBalance(response.jellingReward());
            verify(jellingTransactionRepository).save(any());
        }
    }

    @Nested
    class UpsertChatLogTests {

        static Stream<Arguments> chatJsonScenarios() {
            return Stream.of(
                    Arguments.of("{\"messages\":[{\"role\":\"user\",\"content\":\"질문\"},{\"role\":\"assistant\",\"content\":\"답변\"}]}"),
                    Arguments.of("{\"messages\":[{\"role\":\"user\",\"content\":\"힌트\"},{\"role\":\"assistant\",\"content\":\"안내\"}]}")
            );
        }

        @ParameterizedTest
        @MethodSource("chatJsonScenarios")
        void upsertChatLog_DelegatesTo_UpsertChatJson(String chatJson) {
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();

            quizService.upsertChatLog(userId, quizId, chatJson);

            verify(chatLogRepository, times(1)).upsertChatJson(userId, quizId, chatJson);
            verifyNoMoreInteractions(chatLogRepository);
        }
    }
}
