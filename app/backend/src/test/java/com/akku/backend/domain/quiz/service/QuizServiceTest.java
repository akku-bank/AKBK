package com.akku.backend.domain.quiz.service;

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
import org.springframework.web.client.RestClient;

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
    private JellingRepository jellingRepository;

    @Mock
    private JellingTransactionRepository jellingTransactionRepository;

    @Mock
    private RestClient fastApiClient;

    @Nested
    @DisplayName("1. 퀴즈 조회 및 난이도 락 (fetchQuiz)")
    class FetchQuizTests {

        @Test
        @DisplayName("성공 - 최초 조회 시 난이도 락 생성")
        void fetchQuiz_Success_Initial() {
            // given
            UUID userId = UUID.randomUUID();
            String difficulty = "HIGH";
            Quiz mockQuiz = mock(Quiz.class);
            given(mockQuiz.getId()).willReturn(UUID.randomUUID());
            given(quizRepository.findTopByDifficultyAndCreatedAtBetween(eq(difficulty), any(), any()))
                    .willReturn(Optional.of(mockQuiz));
            given(userQuizRepository.findByUserIdAndQuizId(any(), any())).willReturn(Optional.empty());

            UserQuiz savedUserQuiz = UserQuiz.builder().userId(userId).quizId(mockQuiz.getId()).remainingCredits(100).build();
            given(userQuizRepository.save(any(UserQuiz.class))).willReturn(savedUserQuiz);

            // when
            QuizResponse response = quizService.fetchQuiz(userId, difficulty);

            // then
            assertNotNull(response);
            assertEquals(100, response.remainingCredits());
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

            UserQuiz existingUserQuiz = UserQuiz.builder().userId(userId).quizId(mockQuiz.getId()).remainingCredits(90).build();
            given(userQuizRepository.findByUserIdAndQuizId(any(), any())).willReturn(Optional.of(existingUserQuiz));

            // when & then
            ApiException ex = assertThrows(ApiException.class, () -> quizService.fetchQuiz(userId, difficulty));
            assertEquals(QuizErrorCode.DIFFICULTY_CHANGE_NOT_ALLOWED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("3. 정답 제출 및 보상 (submitAnswer)")
    class SubmitAnswerTests {

        @Test
        @DisplayName("성공 - 정답 시 랜덤 보상 지급 및 지갑 자동 생성")
        void submitAnswer_Correct_Reward() {
            // given
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            AnswerRequest request = new AnswerRequest(quizId, 1);

            UserQuiz userQuiz = UserQuiz.builder().userId(userId).quizId(quizId).remainingCredits(100).build();
            given(userQuizRepository.findByUserIdAndQuizId(userId, quizId)).willReturn(Optional.of(userQuiz));

            Quiz quiz = mock(Quiz.class);
            given(quiz.getCorrectAnswer()).willReturn(1);
            given(quizRepository.findById(quizId)).willReturn(Optional.of(quiz));

            // 지갑이 없는 상태 가정
            given(jellingRepository.findById(userId)).willReturn(Optional.empty());
            Jelling newJelling = Jelling.builder().userId(userId).balance(0).build();
            given(jellingRepository.save(any(Jelling.class))).willReturn(newJelling);

            // when
            AnswerResponse response = quizService.submitAnswer(userId, request);

            // then
            assertTrue(response.isCorrect());
            assertNotNull(response.jellingReward());
            assertTrue(response.jellingReward() >= 1 && response.jellingReward() <= 20);
            verify(jellingRepository).save(any(Jelling.class)); // 지갑 생성 확인
            verify(jellingTransactionRepository).save(any(JellingTransaction.class));
        }

        @Test
        @DisplayName("실패 - 이미 제출한 퀴즈")
        void submitAnswer_Fail_AlreadySubmitted() {
            // given
            UUID userId = UUID.randomUUID();
            UUID quizId = UUID.randomUUID();
            AnswerRequest request = new AnswerRequest(quizId, 1);

            UserQuiz userQuiz = UserQuiz.builder().userId(userId).quizId(quizId).remainingCredits(100).build();
            userQuiz.submit(true); // 이미 제출됨
            given(userQuizRepository.findByUserIdAndQuizId(userId, quizId)).willReturn(Optional.of(userQuiz));

            // when & then
            ApiException ex = assertThrows(ApiException.class, () -> quizService.submitAnswer(userId, request));
            assertEquals(QuizErrorCode.QUIZ_ALREADY_SUBMITTED, ex.getErrorCode());
        }
    }
}
