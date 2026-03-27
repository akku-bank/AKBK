package com.akku.backend.domain.quiz.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.quiz.dto.*;
import com.akku.backend.domain.quiz.entity.*;
import com.akku.backend.domain.quiz.event.QuizChatEvent;
import com.akku.backend.domain.quiz.exception.QuizErrorCode;
import com.akku.backend.domain.quiz.kafka.QuizKafkaProducer;
import com.akku.backend.domain.quiz.repository.*;
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
    private QuizKafkaProducer quizKafkaProducer;
    @Mock
    private Clock clock;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-03-27T10:00:00Z"));
    }

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
            given(userQuizRepository.findTopByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(any(), any(), any()))
                    .willReturn(Optional.empty());
            given(chatLogRepository.findByUserIdAndQuizId(any(), any())).willReturn(Optional.empty());

            UserQuiz savedUserQuiz = UserQuiz.builder().userId(userId).quizId(mockQuiz.getId()).build();
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

            UserQuiz existingUserQuiz = mock(UserQuiz.class);
            given(existingUserQuiz.getRemainingCredits()).willReturn(90);
            given(existingUserQuiz.getQuizId()).willReturn(UUID.randomUUID()); // 다른 난이도의 퀴즈 ID
            given(userQuizRepository.findTopByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(any(), any(), any()))
                    .willReturn(Optional.of(existingUserQuiz));

            // when & then
            ApiException ex = assertThrows(ApiException.class, () -> quizService.fetchQuiz(userId, difficulty));
            assertEquals(QuizErrorCode.DIFFICULTY_CHANGE_NOT_ALLOWED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("2. AI 챗봇 힌트 (chatWithAi)")
    class ChatWithAiTests {

        @Test
        @DisplayName("성공 - Kafka CHAT_REQUEST 이벤트가 올바른 페이로드로 발행됨")
        void chatWithAi_Success_PublishesKafkaEvent() {
            // given
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            String userMessage = "힌트 주세요";
            LocalDate birthDate = LocalDate.of(2015, 3, 10);
            ChatRequest request = new ChatRequest(quizId, userMessage);

            // UserQuiz — @Builder.Default로 remainingCredits=100
            UserQuiz userQuiz = UserQuiz.builder().userId(userId).quizId(quizId).build();
            given(userQuizRepository.findByUserIdAndQuizId(userId, quizId))
                    .willReturn(Optional.of(userQuiz));

            Quiz mockQuiz = mock(Quiz.class);
            given(mockQuiz.getDifficulty()).willReturn("HIGH");
            given(quizRepository.findById(quizId)).willReturn(Optional.of(mockQuiz));

            User mockUser = mock(User.class);
            given(mockUser.getBirthDate()).willReturn(birthDate);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            // when
            quizService.chatWithAi(userId, request);

            // then — Kafka 이벤트 페이로드 전체 검증
            ArgumentCaptor<QuizChatEvent> eventCaptor = ArgumentCaptor.forClass(QuizChatEvent.class);
            verify(quizKafkaProducer).publishChatRequest(eventCaptor.capture());
            QuizChatEvent published = eventCaptor.getValue();

            assertNotNull(published.eventId());            // auto-generated UUID
            assertEquals("CHAT_REQUEST", published.eventType());
            assertEquals(userId,         published.userId());
            assertEquals(quizId,         published.quizId());
            assertEquals(userMessage,    published.message());
            assertEquals("HIGH",         published.difficulty());
            assertEquals(birthDate,      published.birthDate());
            assertEquals(100,            published.remainingCredits());

            // chatWithAi는 더 이상 DB에 직접 쓰지 않음 — 저장은 Kafka 컨슈머 책임
            verifyNoInteractions(chatLogRepository);
        }

        @Test
        @DisplayName("실패 - 이미 제출된 퀴즈에는 AI 힌트 요청 불가 (Freeze)")
        void chatWithAi_Fail_AlreadySubmitted() {
            // given
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            ChatRequest request = new ChatRequest(quizId, "힌트 주세요");

            UserQuiz submittedQuiz = UserQuiz.builder().userId(userId).quizId(quizId).build();
            submittedQuiz.submit(true, LocalDate.now(clock)); // 이미 제출된 상태로 세팅
            given(userQuizRepository.findByUserIdAndQuizId(userId, quizId))
                    .willReturn(Optional.of(submittedQuiz));

            // when & then
            ApiException ex = assertThrows(ApiException.class, () -> quizService.chatWithAi(userId, request));
            assertEquals(QuizErrorCode.QUIZ_ALREADY_SUBMITTED, ex.getErrorCode());

            // Freeze 이후에는 Kafka 이벤트가 발행되지 않아야 함
            verifyNoInteractions(quizKafkaProducer);
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
            given(quiz.getCorrectAnswer()).willReturn(1);
            given(quizRepository.findById(quizId)).willReturn(Optional.of(quiz));

            // when
            AnswerResponse response = quizService.submitAnswer(userId, request);

            // then
            assertTrue(response.isCorrect());
            assertNotNull(response.jellingReward());
            assertTrue(response.jellingReward() >= 1 && response.jellingReward() <= 20);
        }
    }

    @Nested
    @DisplayName("4. 채팅 로그 UPSERT (upsertChatLog)")
    class UpsertChatLogTests {

        static Stream<Arguments> chatJsonScenarios() {
            return Stream.of(
                    Arguments.of(
                            "신규 생성 — 기존 로그 없음",
                            "{\"messages\":[{\"role\":\"user\",\"content\":\"힌트\"},{\"role\":\"assistant\",\"content\":\"설명\"}]}"
                    ),
                    Arguments.of(
                            "업데이트 — 기존 로그 있음",
                            "{\"messages\":[{\"role\":\"user\",\"content\":\"이전 질문\"},{\"role\":\"assistant\",\"content\":\"새 답변\"}]}"
                    )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("chatJsonScenarios")
        @DisplayName("신규·업데이트 모두 upsertChatJson에 위임하고 다른 DB 상호작용은 없다")
        void upsertChatLog_DelegatesTo_UpsertChatJson(String scenario, String chatJson) {
            // given
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();

            // when
            quizService.upsertChatLog(userId, quizId, chatJson);

            // then — INSERT/UPDATE 분기는 DB 레벨(upsertChatJson)이 담당, 서비스는 1회 위임만 한다
            verify(chatLogRepository, times(1)).upsertChatJson(userId, quizId, chatJson);
            verifyNoMoreInteractions(chatLogRepository);
        }
    }
}
