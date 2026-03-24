package com.akku.backend.domain.challenge.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.challenge.dto.EsgChallengeDto;
import com.akku.backend.domain.challenge.entity.EsgChallenge;
import com.akku.backend.domain.challenge.entity.EsgChallengeStatus;
import com.akku.backend.domain.challenge.exception.ChallengeErrorCode;
import com.akku.backend.domain.challenge.repository.EsgChallengeRepository;
import com.akku.backend.domain.jelling.entity.Jelling;
import com.akku.backend.domain.jelling.repository.JellingRepository;
import com.akku.backend.domain.jelling.repository.JellingTransactionRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * EsgChallengeService 단위 테스트.
 *
 * 검증 포인트:
 *  [getThisWeekChallenge]
 *   - 이번 주 챌린지 존재 → INSERT 없이 기존 챌린지 반환
 *   - 이번 주 챌린지 없음 → 신규 생성 후 반환
 *   - 동시 요청 DataIntegrityViolationException → 재조회 후 반환
 *   - CHILD 아닌 유저 → ACCESS_DENIED
 *   - 유저 없음 → USER_NOT_FOUND
 *   - 상태별 boolean 매핑: IN_PROGRESS / SUCCESS / REWARDED
 *
 *  [receiveReward]
 *   - 정상 보상 수령 → REWARDED 상태, remainJelling 반환
 *   - 챌린지 없음 → CHALLENGE_NOT_FOUND
 *   - 다른 유저의 챌린지 → ACCESS_DENIED
 *   - IN_PROGRESS 상태 → INVALID_STATUS_UPDATE
 *   - REWARDED 상태 (이중 수령) → INVALID_STATUS_UPDATE
 *   - Jelling 없음 → JELLING_NOT_FOUND
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EsgChallengeService 단위 테스트")
class EsgChallengeServiceTest {

    @InjectMocks
    private EsgChallengeService esgChallengeService;

    @Mock
    private EsgChallengeRepository esgChallengeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JellingRepository jellingRepository;

    @Mock
    private JellingTransactionRepository jellingTransactionRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // 공통 픽스처 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    private static final LocalDate THIS_MONDAY =
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    private static final LocalDate THIS_SUNDAY = THIS_MONDAY.plusDays(6);

    private User mockChild(UUID childId) {
        User child = mock(User.class);
        given(child.getId()).willReturn(childId);
        given(child.getRole()).willReturn("CHILD");
        return child;
    }

    private User mockParent(UUID parentId) {
        User parent = mock(User.class);
        given(parent.getId()).willReturn(parentId);
        given(parent.getRole()).willReturn("PARENT");
        return parent;
    }

    private EsgChallenge buildChallenge(UUID challengeId, User user, EsgChallengeStatus status) {
        return EsgChallenge.builder()
                .id(challengeId)
                .user(user)
                .rewardAmount(50L)
                .status(status)
                .startDate(THIS_MONDAY)
                .endDate(THIS_SUNDAY)
                .build();
    }

    private Jelling buildJelling(UUID userId, User user, long balance) {
        return Jelling.builder()
                .userId(userId)
                .user(user)
                .balance(balance)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getThisWeekChallenge
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getThisWeekChallenge — 이번 주 ESG 챌린지 조회 (Lazy Insert)")
    class GetThisWeekChallenge {

        @Test
        @DisplayName("이미 생성된 챌린지가 있으면 INSERT 없이 기존 챌린지를 반환한다")
        void returns_existing_challenge_without_insert() {
            // Given
            UUID childId = UUID.randomUUID();
            User child = mockChild(childId);
            EsgChallenge existing = buildChallenge(UUID.randomUUID(), child, EsgChallengeStatus.IN_PROGRESS);

            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            given(esgChallengeRepository.findByUserIdAndStartDate(childId, THIS_MONDAY))
                    .willReturn(Optional.of(existing));

            // When
            EsgChallengeDto.StatusResponse response = esgChallengeService.getThisWeekChallenge(childId);

            // Then
            assertThat(response.getChallengeId()).isEqualTo(existing.getId());
            verify(esgChallengeRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("이번 주 챌린지가 없으면 IN_PROGRESS 상태로 신규 생성 후 반환한다")
        void creates_new_challenge_when_not_exists() {
            // Given
            UUID childId = UUID.randomUUID();
            User child = mockChild(childId);
            EsgChallenge newChallenge = buildChallenge(UUID.randomUUID(), child, EsgChallengeStatus.IN_PROGRESS);

            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            given(esgChallengeRepository.findByUserIdAndStartDate(childId, THIS_MONDAY))
                    .willReturn(Optional.empty());
            given(esgChallengeRepository.saveAndFlush(any(EsgChallenge.class))).willReturn(newChallenge);

            // When
            EsgChallengeDto.StatusResponse response = esgChallengeService.getThisWeekChallenge(childId);

            // Then
            assertThat(response.getChallengeId()).isEqualTo(newChallenge.getId());
            assertThat(response.isCompleted()).isFalse();
            assertThat(response.isRewarded()).isFalse();
            verify(esgChallengeRepository).saveAndFlush(any(EsgChallenge.class));
        }

        @Test
        @DisplayName("동시 요청으로 DataIntegrityViolationException 발생 시 재조회 후 반환한다")
        void fallback_to_find_on_duplicate_key_violation() {
            // Given
            UUID childId = UUID.randomUUID();
            User child = mockChild(childId);
            EsgChallenge existing = buildChallenge(UUID.randomUUID(), child, EsgChallengeStatus.IN_PROGRESS);

            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            given(esgChallengeRepository.findByUserIdAndStartDate(childId, THIS_MONDAY))
                    .willReturn(Optional.empty())                     // 1차 조회: 없음
                    .willReturn(Optional.of(existing));               // 재조회: 반환
            given(esgChallengeRepository.saveAndFlush(any(EsgChallenge.class)))
                    .willThrow(DataIntegrityViolationException.class); // 동시 INSERT 충돌

            // When
            EsgChallengeDto.StatusResponse response = esgChallengeService.getThisWeekChallenge(childId);

            // Then — 재조회 결과가 그대로 반환되어야 함
            assertThat(response.getChallengeId()).isEqualTo(existing.getId());
        }

        @Test
        @DisplayName("CHILD가 아닌 유저(PARENT)가 호출하면 ACCESS_DENIED 예외가 발생한다")
        void throws_access_denied_for_non_child_role() {
            // Given
            UUID parentId = UUID.randomUUID();
            User parent = mockParent(parentId);

            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            // When & Then
            assertThatThrownBy(() -> esgChallengeService.getThisWeekChallenge(parentId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("존재하지 않는 유저 ID로 호출하면 USER_NOT_FOUND 예외가 발생한다")
        void throws_user_not_found_for_unknown_user() {
            // Given
            UUID unknownId = UUID.randomUUID();
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> esgChallengeService.getThisWeekChallenge(unknownId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }

        @Nested
        @DisplayName("status → boolean 매핑 검증")
        class StatusBooleanMapping {

            @Test
            @DisplayName("IN_PROGRESS → isCompleted=false, isRewarded=false")
            void in_progress_mapping() {
                UUID childId = UUID.randomUUID();
                User child = mockChild(childId);
                EsgChallenge challenge = buildChallenge(UUID.randomUUID(), child, EsgChallengeStatus.IN_PROGRESS);

                given(userRepository.findById(childId)).willReturn(Optional.of(child));
                given(esgChallengeRepository.findByUserIdAndStartDate(childId, THIS_MONDAY))
                        .willReturn(Optional.of(challenge));

                EsgChallengeDto.StatusResponse response = esgChallengeService.getThisWeekChallenge(childId);

                assertThat(response.isCompleted()).isFalse();
                assertThat(response.isRewarded()).isFalse();
            }

            @Test
            @DisplayName("SUCCESS → isCompleted=true, isRewarded=false")
            void success_mapping() {
                UUID childId = UUID.randomUUID();
                User child = mockChild(childId);
                EsgChallenge challenge = buildChallenge(UUID.randomUUID(), child, EsgChallengeStatus.SUCCESS);

                given(userRepository.findById(childId)).willReturn(Optional.of(child));
                given(esgChallengeRepository.findByUserIdAndStartDate(childId, THIS_MONDAY))
                        .willReturn(Optional.of(challenge));

                EsgChallengeDto.StatusResponse response = esgChallengeService.getThisWeekChallenge(childId);

                assertThat(response.isCompleted()).isTrue();
                assertThat(response.isRewarded()).isFalse();
            }

            @Test
            @DisplayName("REWARDED → isCompleted=true, isRewarded=true")
            void rewarded_mapping() {
                UUID childId = UUID.randomUUID();
                User child = mockChild(childId);
                EsgChallenge challenge = buildChallenge(UUID.randomUUID(), child, EsgChallengeStatus.REWARDED);

                given(userRepository.findById(childId)).willReturn(Optional.of(child));
                given(esgChallengeRepository.findByUserIdAndStartDate(childId, THIS_MONDAY))
                        .willReturn(Optional.of(challenge));

                EsgChallengeDto.StatusResponse response = esgChallengeService.getThisWeekChallenge(childId);

                assertThat(response.isCompleted()).isTrue();
                assertThat(response.isRewarded()).isTrue();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // receiveReward
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("receiveReward — ESG 챌린지 보상 수령")
    class ReceiveReward {

        @Test
        @DisplayName("SUCCESS 상태의 본인 챌린지 → 젤링 적립 후 REWARDED 상태로 변경, remainJelling 반환")
        void success_challenge_grants_reward_and_marks_rewarded() {
            // Given
            UUID childId = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            User child = mockChild(childId);
            EsgChallenge challenge = buildChallenge(challengeId, child, EsgChallengeStatus.SUCCESS);
            Jelling jelling = buildJelling(childId, child, 100L);

            given(esgChallengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));
            given(jellingRepository.findByUserIdWithLock(childId)).willReturn(Optional.of(jelling));
            given(jellingTransactionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            EsgChallengeDto.RewardResponse response = esgChallengeService.receiveReward(childId, challengeId);

            // Then
            assertThat(response.getChallengeId()).isEqualTo(challengeId);
            assertThat(response.getRemainJelling()).isEqualTo(150L); // 100 + 50 (rewardAmount)
            assertThat(challenge.getStatus()).isEqualTo(EsgChallengeStatus.REWARDED);
            verify(jellingTransactionRepository).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 챌린지 ID → CHALLENGE_NOT_FOUND")
        void throws_not_found_for_unknown_challenge() {
            // Given
            UUID childId = UUID.randomUUID();
            UUID unknownChallengeId = UUID.randomUUID();

            given(esgChallengeRepository.findByIdForUpdate(unknownChallengeId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> esgChallengeService.receiveReward(childId, unknownChallengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.CHALLENGE_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 유저의 챌린지 → ACCESS_DENIED")
        void throws_access_denied_for_other_users_challenge() {
            // Given
            UUID requesterId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User owner = mockChild(ownerId);
            EsgChallenge challenge = buildChallenge(challengeId, owner, EsgChallengeStatus.SUCCESS);

            given(esgChallengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

            // When & Then
            assertThatThrownBy(() -> esgChallengeService.receiveReward(requesterId, challengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("IN_PROGRESS 상태 챌린지 → INVALID_STATUS_UPDATE (아직 달성 전)")
        void throws_invalid_status_for_in_progress_challenge() {
            // Given
            UUID childId = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            User child = mockChild(childId);
            EsgChallenge challenge = buildChallenge(challengeId, child, EsgChallengeStatus.IN_PROGRESS);

            given(esgChallengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

            // When & Then
            assertThatThrownBy(() -> esgChallengeService.receiveReward(childId, challengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        @Test
        @DisplayName("REWARDED 상태 챌린지 → INVALID_STATUS_UPDATE (이중 수령 방지)")
        void throws_invalid_status_for_already_rewarded_challenge() {
            // Given
            UUID childId = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            User child = mockChild(childId);
            EsgChallenge challenge = buildChallenge(challengeId, child, EsgChallengeStatus.REWARDED);

            given(esgChallengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

            // When & Then
            assertThatThrownBy(() -> esgChallengeService.receiveReward(childId, challengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        @Test
        @DisplayName("Jelling 지갑이 없는 유저 → JELLING_NOT_FOUND")
        void throws_jelling_not_found_when_wallet_missing() {
            // Given
            UUID childId = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            User child = mockChild(childId);
            EsgChallenge challenge = buildChallenge(challengeId, child, EsgChallengeStatus.SUCCESS);

            given(esgChallengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));
            given(jellingRepository.findByUserIdWithLock(childId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> esgChallengeService.receiveReward(childId, challengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.JELLING_NOT_FOUND);

            // 젤링 없으면 챌린지 상태는 변경되지 않아야 함
            assertThat(challenge.getStatus()).isEqualTo(EsgChallengeStatus.SUCCESS);
        }
    }
}
