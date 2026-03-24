package com.akku.backend.domain.challenge.service;

import com.akku.backend.domain.bank.exception.BankErrorCode;
import com.akku.backend.domain.bank.service.AccountService;
import com.akku.backend.domain.challenge.dto.SpendingChallengeDto;
import com.akku.backend.domain.challenge.entity.ChallengeStatus;
import com.akku.backend.domain.challenge.entity.SpendingChallenge;
import com.akku.backend.domain.challenge.event.RewardRequestedEvent;
import com.akku.backend.domain.challenge.event.RewardTransferredEvent;
import com.akku.backend.domain.challenge.exception.ChallengeErrorCode;
import com.akku.backend.domain.challenge.repository.SpendingChallengeRepository;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.context.ApplicationEventPublisher;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * SpendingChallengeService 단위 테스트.
 *
 * [Strictness.LENIENT 채택 이유]
 * mockParent / mockChild 헬퍼 메서드가 getId·getRole·getFamilyId 스텁을 미리 설정한다.
 * 그러나 updateChallengeStatus처럼 User 메서드가 실제 호출되지 않는 케이스가 존재하여
 * STRICT_STUBS 기본 모드에서 'unnecessary stubbing' 오류가 발생한다.
 * 공유 헬퍼 기반의 일관된 픽스처 구성을 위해 LENIENT 모드를 명시적으로 채택한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SpendingChallengeService 단위 테스트")
class SpendingChallengeServiceTest {

    @InjectMocks
    private SpendingChallengeService spendingChallengeService;

    @Mock
    private SpendingChallengeRepository spendingChallengeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AccountService accountService;

    // ─────────────────────────────────────────────────────────────────────────
    // 공통 픽스처 헬퍼
    // ─────────────────────────────────────────────────────────────────────────

    /** 서비스 내부 resolveLastSunday() 로직과 동일 — 날짜 하드코딩 없이 동적으로 계산 */
    private static LocalDate resolveLastSunday() {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisMonday.minusDays(1);
    }

    private User mockParent(UUID parentId, UUID familyId) {
        User parent = mock(User.class);
        given(parent.getId()).willReturn(parentId);
        given(parent.getRole()).willReturn("PARENT");
        given(parent.getFamilyId()).willReturn(familyId);
        return parent;
    }

    private User mockChild(UUID childId, UUID familyId) {
        User child = mock(User.class);
        given(child.getId()).willReturn(childId);
        given(child.getRole()).willReturn("CHILD");
        given(child.getFamilyId()).willReturn(familyId);
        return child;
    }

    /**
     * 기본 SpendingChallenge 빌더.
     * startDate 를 '차주 월요일'(미래)로 설정 → 날짜 기한 검증이 통과하도록 보장.
     */
    private SpendingChallenge buildChallenge(UUID id, User child, ChallengeStatus status) {
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return SpendingChallenge.builder()
                .id(id)
                .user(child)
                .subCategoryName("카페")
                .targetSpending(50_000L)
                .rewardAmount(10_000L)
                .status(status)
                .startDate(nextMonday)
                .endDate(nextMonday.plusDays(6))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 그룹 1. 챌린지 생성
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("챌린지 생성 (createChallenge)")
    class CreateChallengeTests {

        @Test
        @DisplayName("1-1. 성공 — PENDING 상태로 저장되고 challengeId 반환")
        void createChallenge_Success() {
            // given
            UUID userId   = UUID.randomUUID();
            UUID savedId  = UUID.randomUUID();

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));
            given(spendingChallengeRepository.existsByUserAndSubCategoryNameAndStartDate(
                    any(User.class), anyString(), any(LocalDate.class)))
                    .willReturn(false);

            SpendingChallenge saved = mock(SpendingChallenge.class);
            given(saved.getId()).willReturn(savedId);
            given(saved.getStatus()).willReturn(ChallengeStatus.PENDING);
            given(spendingChallengeRepository.saveAndFlush(any(SpendingChallenge.class))).willReturn(saved);

            SpendingChallengeDto.CreateRequest request = mock(SpendingChallengeDto.CreateRequest.class);
            given(request.getCategory()).willReturn("카페");
            given(request.getTargetSpending()).willReturn(50_000L);
            given(request.getRewardAmount()).willReturn(10_000L);

            // when
            SpendingChallengeDto.CreateResponse response =
                    spendingChallengeService.createChallenge(userId, request);

            // then
            assertThat(response.getChallengeId()).isEqualTo(savedId);
            assertThat(response.getStatus()).isEqualTo(ChallengeStatus.PENDING.name());
            verify(spendingChallengeRepository).saveAndFlush(any(SpendingChallenge.class));
        }

        @Test
        @DisplayName("1-2. 실패 — 같은 카테고리·날짜 중복 등록 시 DUPLICATE_CHALLENGE 예외, save 미호출")
        void createChallenge_Fail_Duplicate() {
            // given
            UUID userId = UUID.randomUUID();

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));
            given(spendingChallengeRepository.existsByUserAndSubCategoryNameAndStartDate(
                    any(User.class), anyString(), any(LocalDate.class)))
                    .willReturn(true);

            SpendingChallengeDto.CreateRequest request = mock(SpendingChallengeDto.CreateRequest.class);
            given(request.getCategory()).willReturn("카페");

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.createChallenge(userId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.DUPLICATE_CHALLENGE);

            verify(spendingChallengeRepository, never()).saveAndFlush(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 그룹 2. 챌린지 수정
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("챌린지 수정 (updateChallenge)")
    class UpdateChallengeTests {

        @Test
        @DisplayName("2-1. 성공 — PENDING 상태에서 카테고리·목표 변경 후 엔티티 필드 반영")
        void updateChallenge_Success_Pending() {
            // given
            UUID userId      = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));

            SpendingChallenge challenge = buildChallenge(challengeId, child, ChallengeStatus.PENDING);
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));
            // 카테고리 '카페→편의점' 변경 → 중복 검사 호출됨
            given(spendingChallengeRepository.existsByUserAndSubCategoryNameAndStartDate(
                    any(), anyString(), any())).willReturn(false);

            SpendingChallengeDto.UpdateRequest request = mock(SpendingChallengeDto.UpdateRequest.class);
            given(request.getCategory()).willReturn("편의점");
            given(request.getTargetSpending()).willReturn(30_000L);
            given(request.getRewardAmount()).willReturn(5_000L);

            // when
            SpendingChallengeDto.UpdateResponse response =
                    spendingChallengeService.updateChallenge(userId, challengeId, request);

            // then — 응답 ID + Dirty Checking으로 변경된 엔티티 필드 검증
            assertThat(response.getChallengeId()).isEqualTo(challengeId);
            assertThat(challenge.getSubCategoryName()).isEqualTo("편의점");
            assertThat(challenge.getTargetSpending()).isEqualTo(30_000L);
            assertThat(challenge.getRewardAmount()).isEqualTo(5_000L);
        }

        @Test
        @DisplayName("2-2. 성공 — REJECTED 상태에서 수정 시 PENDING 재전환 + 반려 메시지 초기화")
        void updateChallenge_Success_RejectedToPending() {
            // given
            UUID userId      = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));

            // 반려 메시지가 설정된 REJECTED 챌린지
            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(child).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.REJECTED)
                    .startDate(nextMonday).endDate(nextMonday.plusDays(6))
                    .build();
            challenge.replyToChallenge(ChallengeStatus.REJECTED, "목표 금액이 너무 낮아요");
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));
            // 카테고리 동일('카페'→'카페') → 중복 검사 미호출, 스텁 불필요

            SpendingChallengeDto.UpdateRequest request = mock(SpendingChallengeDto.UpdateRequest.class);
            given(request.getCategory()).willReturn("카페");      // 카테고리 동일
            given(request.getTargetSpending()).willReturn(70_000L);
            given(request.getRewardAmount()).willReturn(15_000L);

            // when
            spendingChallengeService.updateChallenge(userId, challengeId, request);

            // then — REJECTED → PENDING 재전환 + 반려 메시지 null 초기화 확인
            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.PENDING);
            assertThat(challenge.getParentMessage()).isNull();
        }

        @Test
        @DisplayName("2-3. 실패 — APPROVED 상태 챌린지 수정 불가 (INVALID_STATUS_UPDATE)")
        void updateChallenge_Fail_WrongStatus() {
            // given
            UUID userId      = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));

            SpendingChallenge challenge = buildChallenge(challengeId, child, ChallengeStatus.APPROVED);
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));

            // when & then — 상태 검증 단계에서 예외 발생, request 메서드 미호출
            assertThatThrownBy(() -> spendingChallengeService.updateChallenge(
                    userId, challengeId, mock(SpendingChallengeDto.UpdateRequest.class)))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        @Test
        @DisplayName("2-4. 실패 — 타인의 챌린지 수정 시도 (ACCESS_DENIED)")
        void updateChallenge_Fail_NotOwner() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID ownerId     = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User requester = mockChild(requesterId, UUID.randomUUID());
            given(userRepository.findById(requesterId)).willReturn(Optional.of(requester));

            // 챌린지 소유자는 requester와 다른 userId
            User owner = mockChild(ownerId, UUID.randomUUID());
            given(spendingChallengeRepository.findById(challengeId))
                    .willReturn(Optional.of(buildChallenge(challengeId, owner, ChallengeStatus.PENDING)));

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.updateChallenge(
                    requesterId, challengeId, mock(SpendingChallengeDto.UpdateRequest.class)))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("2-5. 실패 — 챌린지 시작일이 이미 도래한 경우 수정 불가 (INVALID_STATUS_UPDATE)")
        void updateChallenge_Fail_StartDatePassed() {
            // given
            UUID userId      = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));

            // startDate를 3일 전(과거)으로 설정 → 해당 주간이 이미 시작됨
            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(child).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.PENDING)
                    .startDate(LocalDate.now().minusDays(3))
                    .endDate(LocalDate.now().plusDays(3))
                    .build();
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));

            // when & then — 날짜 검증에서 예외, request.getCategory() 미호출
            assertThatThrownBy(() -> spendingChallengeService.updateChallenge(
                    userId, challengeId, mock(SpendingChallengeDto.UpdateRequest.class)))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 그룹 3. 챌린지 삭제
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("챌린지 삭제 (deleteChallenge)")
    class DeleteChallengeTests {

        @Test
        @DisplayName("3-1. 성공 — PENDING 상태 챌린지 Hard Delete 호출 확인")
        void deleteChallenge_Success() {
            // given
            UUID userId      = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));

            SpendingChallenge challenge = buildChallenge(challengeId, child, ChallengeStatus.PENDING);
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));

            // when
            spendingChallengeService.deleteChallenge(userId, challengeId);

            // then
            verify(spendingChallengeRepository).delete(challenge);
        }

        @Test
        @DisplayName("3-2. 실패 — IN_PROGRESS 상태 챌린지 삭제 불가 (INVALID_STATUS_UPDATE), delete 미호출")
        void deleteChallenge_Fail_WrongStatus() {
            // given
            UUID userId      = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));

            given(spendingChallengeRepository.findById(challengeId))
                    .willReturn(Optional.of(buildChallenge(challengeId, child, ChallengeStatus.IN_PROGRESS)));

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.deleteChallenge(userId, challengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.INVALID_STATUS_UPDATE);

            verify(spendingChallengeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("3-3. 실패 — 타인의 챌린지 삭제 시도 (ACCESS_DENIED), delete 미호출")
        void deleteChallenge_Fail_NotOwner() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID ownerId     = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User requester = mockChild(requesterId, UUID.randomUUID());
            given(userRepository.findById(requesterId)).willReturn(Optional.of(requester));

            User owner = mockChild(ownerId, UUID.randomUUID());
            given(spendingChallengeRepository.findById(challengeId))
                    .willReturn(Optional.of(buildChallenge(challengeId, owner, ChallengeStatus.PENDING)));

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.deleteChallenge(requesterId, challengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.ACCESS_DENIED);

            verify(spendingChallengeRepository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 그룹 4. 챌린지 승인/반려
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("챌린지 승인/반려 (updateChallengeStatus)")
    class StatusUpdateTests {

        @Test
        @DisplayName("4-1. 성공 — 부모가 PENDING 챌린지를 APPROVED로 전환, 응원 메시지 저장")
        void updateChallengeStatus_Approve_Success() {
            // given
            UUID parentId    = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            UUID familyId    = UUID.randomUUID();

            User parent = mockParent(parentId, familyId);
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(UUID.randomUUID(), familyId);
            SpendingChallenge challenge = buildChallenge(challengeId, child, ChallengeStatus.PENDING);
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));

            SpendingChallengeDto.StatusUpdateRequest request = mock(SpendingChallengeDto.StatusUpdateRequest.class);
            given(request.getStatus()).willReturn(ChallengeStatus.APPROVED);
            given(request.getParentMessage()).willReturn("화이팅! 잘 해보자!");

            // when
            SpendingChallengeDto.StatusUpdateResponse response =
                    spendingChallengeService.updateChallengeStatus(parentId, challengeId, request);

            // then — 응답 DTO + 엔티티 상태·메시지 변경 확인
            assertThat(response.getStatus()).isEqualTo(ChallengeStatus.APPROVED.name());
            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.APPROVED);
            assertThat(challenge.getParentMessage()).isEqualTo("화이팅! 잘 해보자!");
        }

        @Test
        @DisplayName("4-2. 성공 — 부모가 PENDING 챌린지를 REJECTED + 반려 메시지와 함께 처리")
        void updateChallengeStatus_Reject_WithMessage() {
            // given
            UUID parentId    = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            UUID familyId    = UUID.randomUUID();

            User parent = mockParent(parentId, familyId);
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(UUID.randomUUID(), familyId);
            SpendingChallenge challenge = buildChallenge(challengeId, child, ChallengeStatus.PENDING);
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));

            SpendingChallengeDto.StatusUpdateRequest request = mock(SpendingChallengeDto.StatusUpdateRequest.class);
            given(request.getStatus()).willReturn(ChallengeStatus.REJECTED);
            given(request.getParentMessage()).willReturn("목표 금액이 너무 낮아요!");

            // when
            spendingChallengeService.updateChallengeStatus(parentId, challengeId, request);

            // then — REJECTED 전환 + 반려 메시지 저장 확인
            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.REJECTED);
            assertThat(challenge.getParentMessage()).isEqualTo("목표 금액이 너무 낮아요!");
        }

        @Test
        @DisplayName("4-3. 실패 — 이미 IN_PROGRESS 챌린지 승인 시도 (INVALID_STATUS_UPDATE)")
        void updateChallengeStatus_Fail_NotPending() {
            // given
            UUID parentId    = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            UUID familyId    = UUID.randomUUID();

            User parent = mockParent(parentId, familyId);
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(UUID.randomUUID(), familyId);
            given(spendingChallengeRepository.findById(challengeId))
                    .willReturn(Optional.of(buildChallenge(challengeId, child, ChallengeStatus.IN_PROGRESS)));

            SpendingChallengeDto.StatusUpdateRequest request = mock(SpendingChallengeDto.StatusUpdateRequest.class);
            // APPROVED는 유효한 값이지만 대상 챌린지가 PENDING이 아님 → step 5에서 예외
            given(request.getStatus()).willReturn(ChallengeStatus.APPROVED);

            // when & then
            assertThatThrownBy(() ->
                    spendingChallengeService.updateChallengeStatus(parentId, challengeId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }

        @Test
        @DisplayName("4-4. 실패 — APPROVED·REJECTED 외 상태값(IN_PROGRESS) 요청 (INVALID_STATUS_UPDATE)")
        void updateChallengeStatus_Fail_InvalidStatusValue() {
            // given
            UUID parentId    = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            UUID familyId    = UUID.randomUUID();

            User parent = mockParent(parentId, familyId);
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(UUID.randomUUID(), familyId);
            given(spendingChallengeRepository.findById(challengeId))
                    .willReturn(Optional.of(buildChallenge(challengeId, child, ChallengeStatus.PENDING)));

            SpendingChallengeDto.StatusUpdateRequest request = mock(SpendingChallengeDto.StatusUpdateRequest.class);
            given(request.getStatus()).willReturn(ChallengeStatus.IN_PROGRESS); // 허용되지 않는 값

            // when & then — 상태값 유효성 검증 단계(step 4)에서 즉시 예외
            assertThatThrownBy(() ->
                    spendingChallengeService.updateChallengeStatus(parentId, challengeId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.INVALID_STATUS_UPDATE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 그룹 5. 차주 목록 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("차주 목록 조회 (getNextWeekChallenges)")
    class GetNextWeekChallengesTests {

        @Test
        @DisplayName("5-1. CHILD 역할 — childId 없이 본인 데이터로 조회, 상태 필터 미적용 쿼리 호출")
        void getNextWeekChallenges_Child_SelfQuery() {
            // given
            UUID userId   = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();

            User child = mockChild(userId, familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(child));
            given(spendingChallengeRepository.findAllByUserAndStartDate(
                    eq(child), any(LocalDate.class))).willReturn(List.of());

            // when
            SpendingChallengeDto.ListResponse response =
                    spendingChallengeService.getNextWeekChallenges(userId, null, null);

            // then — 빈 목록 + 상태 필터 없는 쿼리 메서드 호출 확인
            assertThat(response.getChallenges()).isEmpty();
            verify(spendingChallengeRepository)
                    .findAllByUserAndStartDate(eq(child), any(LocalDate.class));
        }

        @Test
        @DisplayName("5-2. PARENT 역할 — 같은 가족 자녀 지정 조회 + PENDING 상태 필터 적용")
        void getNextWeekChallenges_Parent_WithStatusFilter() {
            // given
            UUID parentId = UUID.randomUUID();
            UUID childId  = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();

            User parent = mockParent(parentId, familyId);
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(childId, familyId);
            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            given(spendingChallengeRepository.findAllByUserAndStartDateAndStatus(
                    eq(child), any(LocalDate.class), eq(ChallengeStatus.PENDING)))
                    .willReturn(List.of());

            // when
            spendingChallengeService.getNextWeekChallenges(parentId, childId, ChallengeStatus.PENDING);

            // then — status 필터 포함 쿼리 메서드 호출 확인 (자녀 searchUser 기준)
            verify(spendingChallengeRepository).findAllByUserAndStartDateAndStatus(
                    eq(child), any(LocalDate.class), eq(ChallengeStatus.PENDING));
        }

        @Test
        @DisplayName("5-3. 실패 — PARENT가 다른 가족 자녀 조회 시도 (ACCESS_DENIED)")
        void getNextWeekChallenges_Parent_DifferentFamily() {
            // given — 부모와 자녀의 familyId를 의도적으로 다르게 설정
            UUID parentId = UUID.randomUUID();
            UUID childId  = UUID.randomUUID();

            User parent = mockParent(parentId, UUID.randomUUID()); // familyId-A
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(childId, UUID.randomUUID());   // familyId-B
            given(userRepository.findById(childId)).willReturn(Optional.of(child));

            // when & then
            assertThatThrownBy(() ->
                    spendingChallengeService.getNextWeekChallenges(parentId, childId, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.ACCESS_DENIED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 그룹 6. 단건 상세 조회
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("단건 상세 조회 (getChallengeDetail)")
    class GetChallengeDetailTests {

        @Test
        @DisplayName("6-1. calculateCurrentSpending Native Query 결과가 응답 DTO의 currentSpending에 매핑됨")
        void getChallengeDetail_CurrentSpendingMapped() {
            // given
            UUID childId     = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            UUID familyId    = UUID.randomUUID();
            Long expected    = 32_000L;

            User child = mockChild(childId, familyId);
            given(userRepository.findById(childId)).willReturn(Optional.of(child));

            SpendingChallenge challenge = buildChallenge(challengeId, child, ChallengeStatus.IN_PROGRESS);
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));
            given(spendingChallengeRepository.calculateCurrentSpending(
                    eq(childId), eq("카페"), any(), any())).willReturn(expected);

            // when
            SpendingChallengeDto.DetailResponse response =
                    spendingChallengeService.getChallengeDetail(childId, challengeId);

            // then — Native Query 결과가 응답 필드에 정확히 매핑됨
            assertThat(response.getCurrentSpending()).isEqualTo(expected);
            verify(spendingChallengeRepository)
                    .calculateCurrentSpending(eq(childId), eq("카페"), any(), any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 그룹 7. 보상 요청
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("보상 요청 (requestReward)")
    class RequestRewardTests {

        @Test
        @DisplayName("7-1. 성공 — SUCCESS + 지난주 endDate → REWARD_REQUESTED 전환 + 부모 FCM 이벤트 발행·페이로드 검증")
        void requestReward_Success() {
            // given
            UUID userId      = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            UUID familyId    = UUID.randomUUID();
            LocalDate lastSunday = resolveLastSunday(); // 서비스 내부와 동일한 날짜 계산

            User child = mockChild(userId, familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(child));

            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(child).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.SUCCESS)
                    .startDate(lastSunday.minusDays(6))
                    .endDate(lastSunday)  // 정확히 '지난주 일요일'
                    .build();
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));

            // when
            SpendingChallengeDto.RewardRequestResponse response =
                    spendingChallengeService.requestReward(userId, challengeId);

            // then — 상태 변경 확인
            assertThat(response.getStatus()).isEqualTo(ChallengeStatus.REWARD_REQUESTED.name());
            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.REWARD_REQUESTED);

            // then — 부모 FCM 이벤트 발행 + 페이로드 핵심 필드 검증
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(RewardRequestedEvent.class);
            RewardRequestedEvent event = (RewardRequestedEvent) captor.getValue();
            assertThat(event.challengeId()).isEqualTo(challengeId);
            assertThat(event.familyId()).isEqualTo(familyId);
            assertThat(event.rewardAmount()).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("7-2. 실패 — FAIL 상태 챌린지 보상 요청 (INVALID_STATUS_UPDATE), 이벤트 미발행")
        void requestReward_Fail_WrongStatus() {
            // given
            UUID userId      = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            LocalDate lastSunday = resolveLastSunday();

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));

            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(child).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.FAIL)
                    .startDate(lastSunday.minusDays(6)).endDate(lastSunday)
                    .build();
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.requestReward(userId, challengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.INVALID_STATUS_UPDATE);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("7-3. 실패 — endDate가 지난주 일요일 아닌 기한 만료 챌린지 (REWARD_PERIOD_EXPIRED)")
        void requestReward_Fail_PeriodExpired() {
            // given
            UUID userId      = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User child = mockChild(userId, UUID.randomUUID());
            given(userRepository.findById(userId)).willReturn(Optional.of(child));

            // endDate = 2주 전 일요일 → 보상 요청 기한 만료
            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(child).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.SUCCESS)
                    .startDate(LocalDate.now().minusDays(13))
                    .endDate(LocalDate.now().minusDays(7))
                    .build();
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.requestReward(userId, challengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.REWARD_PERIOD_EXPIRED);
        }

        @Test
        @DisplayName("7-4. 실패 — 타인의 챌린지에 보상 요청 (ACCESS_DENIED)")
        void requestReward_Fail_NotOwner() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID ownerId     = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            LocalDate lastSunday = resolveLastSunday();

            User requester = mockChild(requesterId, UUID.randomUUID());
            given(userRepository.findById(requesterId)).willReturn(Optional.of(requester));

            User owner = mockChild(ownerId, UUID.randomUUID());
            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(owner).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.SUCCESS)
                    .startDate(lastSunday.minusDays(6)).endDate(lastSunday)
                    .build();
            given(spendingChallengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.requestReward(requesterId, challengeId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.ACCESS_DENIED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 그룹 8. 보상 송금 처리
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("보상 송금 처리 (processRewardTransfer)")
    class ProcessRewardTransferTests {

        @Test
        @DisplayName("8-1. 성공 — REWARDED 전환 + internalRewardTransfer 호출(금액·적요) + 자녀 FCM 이벤트 페이로드 검증")
        void processRewardTransfer_Success() {
            // given
            UUID parentId        = UUID.randomUUID();
            UUID childId         = UUID.randomUUID();
            UUID challengeId     = UUID.randomUUID();
            UUID familyId        = UUID.randomUUID();
            UUID parentAccountId = UUID.randomUUID();
            UUID childAccountId  = UUID.randomUUID();

            User parent = mockParent(parentId, familyId);
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(childId, familyId);
            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(child).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.REWARD_REQUESTED)
                    .startDate(LocalDate.now().minusDays(6)).endDate(LocalDate.now())
                    .build();
            given(spendingChallengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

            SpendingChallengeDto.RewardTransferRequest request = mock(SpendingChallengeDto.RewardTransferRequest.class);
            given(request.getParentAccountId()).willReturn(parentAccountId);
            given(request.getChildAccountId()).willReturn(childAccountId);
            // internalRewardTransfer = void → 기본 동작(do nothing)으로 성공 시뮬레이션

            // when
            SpendingChallengeDto.RewardTransferResponse response =
                    spendingChallengeService.processRewardTransfer(parentId, challengeId, request);

            // then — 응답 DTO + 엔티티 상태 변경 확인
            assertThat(response.getStatus()).isEqualTo(ChallengeStatus.REWARDED.name());
            assertThat(response.getRewardAmount()).isEqualTo(10_000L);
            assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.REWARDED);

            // then — 송금 API 호출 검증 (parentId, childId, accountId, 금액 + 카테고리명 포함 적요)
            verify(accountService).internalRewardTransfer(
                    eq(parentId), eq(childId), eq(parentAccountId), eq(childAccountId), eq(10_000L),
                    contains("카페"),   // depositMemo: "주간 소비 챌린지 보상 (카페)"
                    contains("카페")); // withdrawalMemo: "챌린지 보상 송금 (카페)"

            // then — 자녀 FCM 이벤트 발행 + 페이로드 검증
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(RewardTransferredEvent.class);
            RewardTransferredEvent event = (RewardTransferredEvent) captor.getValue();
            assertThat(event.challengeId()).isEqualTo(challengeId);
            assertThat(event.childId()).isEqualTo(childId);
            assertThat(event.rewardAmount()).isEqualTo(10_000L);
            assertThat(event.category()).isEqualTo("카페");
        }

        @Test
        @DisplayName("8-2. [롤백 방어] 잔액 부족 예외 발생 시 챌린지 상태가 REWARD_REQUESTED로 유지")
        void processRewardTransfer_Fail_InsufficientBalance_StatusNotChanged() {
            // given
            UUID parentId    = UUID.randomUUID();
            UUID childId     = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            UUID familyId    = UUID.randomUUID();

            User parent = mockParent(parentId, familyId);
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(childId, familyId);
            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(child).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.REWARD_REQUESTED)
                    .startDate(LocalDate.now().minusDays(6)).endDate(LocalDate.now())
                    .build();
            given(spendingChallengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

            SpendingChallengeDto.RewardTransferRequest request = mock(SpendingChallengeDto.RewardTransferRequest.class);
            given(request.getParentAccountId()).willReturn(UUID.randomUUID());
            given(request.getChildAccountId()).willReturn(UUID.randomUUID());

            // internalRewardTransfer가 잔액 부족 예외를 발생시키도록 설정
            willThrow(new ApiException(BankErrorCode.INSUFFICIENT_BALANCE))
                    .given(accountService)
                    .internalRewardTransfer(any(), any(), any(), any(), any(), anyString(), anyString());

            // when — 예외가 호출자에게 전파됨
            assertThatThrownBy(() -> spendingChallengeService.processRewardTransfer(parentId, challengeId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(BankErrorCode.INSUFFICIENT_BALANCE);

            // then ★ 핵심 롤백 방어 검증
            //   송금 예외로 인해 updateStatus(REWARDED)가 실행되지 않았으므로 상태가 유지됨
            assertThat(challenge.getStatus())
                    .as("송금 실패 시 챌린지 상태는 REWARDED로 변경되지 않아야 한다")
                    .isEqualTo(ChallengeStatus.REWARD_REQUESTED);

            // then — FCM 이벤트도 발행되지 않아야 함
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("8-3. 실패 — 다른 가족 부모의 송금 시도 (ACCESS_DENIED), 송금 API 미호출")
        void processRewardTransfer_Fail_DifferentFamily() {
            // given — 부모와 자녀의 familyId를 의도적으로 다르게 설정
            UUID parentId    = UUID.randomUUID();
            UUID childId     = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();

            User parent = mockParent(parentId, UUID.randomUUID()); // familyId-A
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(childId, UUID.randomUUID());    // familyId-B
            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(child).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.REWARD_REQUESTED)
                    .startDate(LocalDate.now().minusDays(6)).endDate(LocalDate.now())
                    .build();
            given(spendingChallengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

            SpendingChallengeDto.RewardTransferRequest request = mock(SpendingChallengeDto.RewardTransferRequest.class);

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.processRewardTransfer(parentId, challengeId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.ACCESS_DENIED);

            // then — 가족 검증 실패로 송금 API 절대 미호출
            verify(accountService, never())
                    .internalRewardTransfer(any(), any(), any(), any(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("8-4. [이중 출금 방지] 이미 REWARDED 상태 재요청 → INVALID_STATUS_UPDATE, 송금 API 미호출")
        void processRewardTransfer_Fail_AlreadyRewarded() {
            // given
            UUID parentId    = UUID.randomUUID();
            UUID childId     = UUID.randomUUID();
            UUID challengeId = UUID.randomUUID();
            UUID familyId    = UUID.randomUUID();

            User parent = mockParent(parentId, familyId);
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            User child = mockChild(childId, familyId);
            SpendingChallenge challenge = SpendingChallenge.builder()
                    .id(challengeId).user(child).subCategoryName("카페")
                    .targetSpending(50_000L).rewardAmount(10_000L)
                    .status(ChallengeStatus.REWARDED)  // 이미 보상 완료
                    .startDate(LocalDate.now().minusDays(6)).endDate(LocalDate.now())
                    .build();
            given(spendingChallengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

            SpendingChallengeDto.RewardTransferRequest request = mock(SpendingChallengeDto.RewardTransferRequest.class);

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.processRewardTransfer(parentId, challengeId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.INVALID_STATUS_UPDATE);

            // then ★ 이중 출금 방지 가드: REWARDED 상태 검증에서 막혀 송금 API 절대 미호출
            verify(accountService, never())
                    .internalRewardTransfer(any(), any(), any(), any(), any(), anyString(), anyString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. getThisWeekChallenges (WeeklyChallengesFacade 전용)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("9. getThisWeekChallenges — 이번 주 소비 챌린지 목록 조회 (자녀 전용)")
    class GetThisWeekChallenges {

        private static final LocalDate THIS_MONDAY =
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        private static final LocalDate THIS_SUNDAY = THIS_MONDAY.plusDays(6);

        @Test
        @DisplayName("9-1. 이번 주 챌린지 2건 → ChallengeSummary 리스트로 반환된다")
        void returns_this_week_challenges_as_summaries() {
            // given
            UUID childId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();
            User child = mockChild(childId, familyId);

            SpendingChallenge cafe = SpendingChallenge.builder()
                    .id(UUID.randomUUID()).user(child).subCategoryName("카페")
                    .targetSpending(30_000L).rewardAmount(5_000L)
                    .status(ChallengeStatus.IN_PROGRESS)
                    .startDate(THIS_MONDAY).endDate(THIS_SUNDAY).build();

            SpendingChallenge grocery = SpendingChallenge.builder()
                    .id(UUID.randomUUID()).user(child).subCategoryName("마트")
                    .targetSpending(50_000L).rewardAmount(8_000L)
                    .status(ChallengeStatus.IN_PROGRESS)
                    .startDate(THIS_MONDAY).endDate(THIS_SUNDAY).build();

            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            given(spendingChallengeRepository.findAllByUserAndStartDate(child, THIS_MONDAY))
                    .willReturn(List.of(cafe, grocery));

            // when
            List<SpendingChallengeDto.ChallengeSummary> result =
                    spendingChallengeService.getThisWeekChallenges(childId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(SpendingChallengeDto.ChallengeSummary::getCategory)
                    .containsExactlyInAnyOrder("카페", "마트");
            assertThat(result)
                    .extracting(SpendingChallengeDto.ChallengeSummary::getStartDate)
                    .allMatch(d -> d.equals(THIS_MONDAY));
        }

        @Test
        @DisplayName("9-2. 이번 주 챌린지 0건 → 빈 리스트 반환")
        void returns_empty_list_when_no_challenge_this_week() {
            // given
            UUID childId = UUID.randomUUID();
            User child = mockChild(childId, UUID.randomUUID());

            given(userRepository.findById(childId)).willReturn(Optional.of(child));
            given(spendingChallengeRepository.findAllByUserAndStartDate(child, THIS_MONDAY))
                    .willReturn(List.of());

            // when
            List<SpendingChallengeDto.ChallengeSummary> result =
                    spendingChallengeService.getThisWeekChallenges(childId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("9-3. PARENT 역할 유저 호출 → ACCESS_DENIED")
        void throws_access_denied_for_parent_role() {
            // given
            UUID parentId = UUID.randomUUID();
            User parent = mockParent(parentId, UUID.randomUUID());
            given(userRepository.findById(parentId)).willReturn(Optional.of(parent));

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.getThisWeekChallenges(parentId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(ChallengeErrorCode.ACCESS_DENIED);
        }

        @Test
        @DisplayName("9-4. 존재하지 않는 유저 → USER_NOT_FOUND")
        void throws_user_not_found_for_unknown_user() {
            // given
            UUID unknownId = UUID.randomUUID();
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> spendingChallengeService.getThisWeekChallenges(unknownId))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getErrorCode())
                    .isEqualTo(com.akku.backend.domain.user.exception.UserErrorCode.USER_NOT_FOUND);
        }
    }
}
