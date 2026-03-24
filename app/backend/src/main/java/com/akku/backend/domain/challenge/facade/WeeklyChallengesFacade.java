package com.akku.backend.domain.challenge.facade;

import com.akku.backend.domain.challenge.dto.EsgChallengeDto;
import com.akku.backend.domain.challenge.dto.SpendingChallengeDto;
import com.akku.backend.domain.challenge.dto.WeeklyChallengesDto;
import com.akku.backend.domain.challenge.service.EsgChallengeService;
import com.akku.backend.domain.challenge.service.SpendingChallengeService;
import com.akku.backend.domain.quiz.dto.QuizWeeklyStatusDto;
import com.akku.backend.domain.quiz.service.QuizWeeklyStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 이번 주 전체 챌린지 종합 조회 Facade.
 *
 * <p>세 도메인 서비스를 조합하여 단일 응답을 조립한다.
 *
 * <h3>트랜잭션 전략</h3>
 * 이 Facade 에는 {@code @Transactional} 을 걸지 않는다.
 * 이유: {@link EsgChallengeService#getThisWeekChallenge} 는 {@code @Transactional}(쓰기)이고,
 * 나머지 두 서비스는 {@code readOnly=true} 트랜잭션을 사용한다.
 * Facade 에 readOnly 트랜잭션을 걸면 ESG Lazy Insert 가 오염되므로,
 * 각 서비스가 독립적으로 자신의 트랜잭션 경계를 관리하도록 위임한다.
 *
 * <h3>의존성 방향</h3>
 * <pre>
 * WeeklyChallengesFacade (challenge)
 *   ├── EsgChallengeService      (challenge)  @Transactional        — Lazy Insert 포함
 *   ├── SpendingChallengeService (challenge)  @Transactional(readOnly=true)
 *   └── QuizWeeklyStatusService  (quiz)       @Transactional(readOnly=true)
 * </pre>
 * challenge → quiz 단방향 의존. quiz 도메인은 challenge 를 참조하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class WeeklyChallengesFacade {

    private final EsgChallengeService esgChallengeService;
    private final SpendingChallengeService spendingChallengeService;
    private final QuizWeeklyStatusService quizWeeklyStatusService;

    /**
     * 이번 주 전체 챌린지 상태를 조합하여 반환한다.
     *
     * <p>호출 순서:
     * <ol>
     *   <li>ESG  — getThisWeekChallenge (없으면 Lazy Insert)</li>
     *   <li>Spending — getThisWeekChallenges (이번 주 목록, 0개 이상)</li>
     *   <li>Quiz — getThisWeekStatus (이번 주 퀴즈 완료 집계)</li>
     * </ol>
     *
     * @param userId CHILD 자녀 ID (Controller 에서 인증된 값)
     */
    public WeeklyChallengesDto.Response getWeeklyChallenges(UUID userId) {
        EsgChallengeDto.StatusResponse esg =
                esgChallengeService.getThisWeekChallenge(userId);

        List<SpendingChallengeDto.ChallengeSummary> spending =
                spendingChallengeService.getThisWeekChallenges(userId);

        QuizWeeklyStatusDto quiz =
                quizWeeklyStatusService.getThisWeekStatus(userId);

        return WeeklyChallengesDto.Response.builder()
                .esg(esg)
                .spending(spending)
                .quiz(quiz)
                .build();
    }
}
