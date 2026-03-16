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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

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
            submittedQuiz.submit(true); // 이미 제출된 상태로 세팅
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

        @Test
        @DisplayName("신규 생성 - 기존 로그가 없으면 새 ChatLog를 저장한다")
        void upsertChatLog_Create_WhenNoExistingLog() {
            // given
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            String chatJson = "{\"messages\":[{\"role\":\"user\",\"content\":\"힌트\"},{\"role\":\"assistant\",\"content\":\"설명\"}]}";
            given(chatLogRepository.findByUserIdAndQuizId(userId, quizId)).willReturn(Optional.empty());

            // when
            quizService.upsertChatLog(userId, quizId, chatJson);

            // then — 새 엔티티로 save 호출, chatJson 일치 확인
            ArgumentCaptor<ChatLog> captor = ArgumentCaptor.forClass(ChatLog.class);
            verify(chatLogRepository).save(captor.capture());
            assertEquals(chatJson, captor.getValue().getChatJson());
        }

        @Test
        @DisplayName("업데이트 - 기존 로그가 있으면 chatJson을 갱신한 뒤 save한다")
        void upsertChatLog_Update_WhenExistingLog() {
            // given
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            String oldChatJson = "{\"messages\":[{\"role\":\"user\",\"content\":\"이전 질문\"}]}";
            String newChatJson = "{\"messages\":[{\"role\":\"user\",\"content\":\"이전 질문\"},{\"role\":\"assistant\",\"content\":\"새 답변\"}]}";

            ChatLog existingLog = ChatLog.builder().userId(userId).quizId(quizId).chatJson(oldChatJson).build();
            given(chatLogRepository.findByUserIdAndQuizId(userId, quizId)).willReturn(Optional.of(existingLog));

            // when
            quizService.upsertChatLog(userId, quizId, newChatJson);

            // then — 동일 인스턴스를 갱신 후 save, chatJson이 교체됐는지 확인
            verify(chatLogRepository).save(existingLog);
            assertEquals(newChatJson, existingLog.getChatJson());
        }
    }
}
