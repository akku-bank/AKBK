package com.akku.backend.domain.family.service;

import com.akku.backend.domain.family.dto.*;
import com.akku.backend.domain.family.entity.FamilyEntity;
import com.akku.backend.domain.family.entity.FamilyProfileEntity;
import com.akku.backend.domain.family.repository.FamilyProfileRepository;
import com.akku.backend.domain.family.repository.FamilyRepository;
import com.akku.backend.domain.family.exception.FamilyErrorCode;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyProfileRepository familyProfileRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final Clock clock;

    // ── 헬퍼: userId → User 조회 후 familyId 반환. 가족 미가입 시 FAMILY_NOT_FOUND ──
    private UUID resolveFamilyId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
        UUID familyId = user.getFamilyId();
        if (familyId == null) {
            throw new ApiException(FamilyErrorCode.FAMILY_NOT_FOUND);
        }
        return familyId;
    }

    /**
     * 1. 가족 그룹 생성 (최초 빈 그룹 생성)
     *
     * @param parentId 가족 그룹을 생성하는 부모의 ID
     * @return 생성된 가족 그룹의 고유 ID
     */
    @Transactional
    public FamilyCreateResponse createFamilyGroup(UUID parentId) {

        // 1. 부모 유저 조회 — save 이전에 검증하여 불필요한 DB 저장을 방지
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 2. 이미 가족 그룹에 소속된 경우 중복 생성 방지
        if (parent.getFamilyId() != null) {
            throw new ApiException(FamilyErrorCode.USER_ALREADY_IN_FAMILY);
        }

        // 3. 빈 가족 그룹 엔티티 생성 및 DB 저장 (QR 코드는 별도 API로 발급)
        FamilyEntity newFamily = FamilyEntity.builder().build();
        FamilyEntity savedFamily = familyRepository.save(newFamily);

        // 4. 부모(User)의 familyId를 방금 생성한 그룹의 ID로 업데이트
        parent.updateFamilyId(savedFamily.getId());

        return new FamilyCreateResponse(savedFamily.getId());
    }

    /**
     * 1-1. 가족 구성원 사전 등록 (미연동 프로필 생성)
     * 부모가 자녀의 이름과 생년월일을 미리 등록하여 가입 대기 상태의 프로필을 생성
     */
    @Transactional
    public void preRegisterFamilyMember(UUID userId, FamilyMemberPreRegisterRequest request) {
        UUID familyId = resolveFamilyId(userId);

        FamilyProfileEntity profile = FamilyProfileEntity.builder()
                .familyId(familyId)
                .name(request.name())
                .role(request.role())
                .birthDate(request.birthDate())
                .build();

        familyProfileRepository.save(profile);
    }

    /**
     * 2. 가족 QR 발급 및 조회 (GET /api/families/qr)
     * 이미 유효한 QR이 있으면 반환하고, 없거나 만료됐으면 새로 생성
     */
    @Transactional
    public FamilyQrResponse getOrGenerateFamilyQr(UUID userId) {
        UUID familyId = resolveFamilyId(userId);

        FamilyEntity family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.FAMILY_NOT_FOUND));

        if (family.getQrCode() != null &&
                family.getQrExpiresAt() != null &&
                family.getQrExpiresAt().isAfter(LocalDateTime.now(clock))) {

            return new FamilyQrResponse(family.getQrCode(), family.getQrExpiresAt());
        }

        String newQrCode = UUID.randomUUID().toString();
        LocalDateTime newExpiresAt = LocalDateTime.now(clock).plusMinutes(5);

        family.updateQrCode(newQrCode, newExpiresAt);

        return new FamilyQrResponse(newQrCode, newExpiresAt);
    }

    /**
     * 3. 가족 그룹 합류 (QR 스캔 + 이름/생일 기반 자동 연동)
     * 이름·생일은 JWT에서 전달받지 않고 User 엔티티에서 직접 조회
     */
    @Transactional
    public void joinFamilyGroup(UUID childId, String scannedQrCode) {
        // 1. 자녀 유저 조회 — 이후 로직 진행 전에 먼저 유효성 검증 (Fail-Fast)
        User child = userRepository.findById(childId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 2. 이미 다른 가족 그룹에 소속된 경우 합류 불가 (FamilyProfile 오염 방어)
        if (child.getFamilyId() != null) {
            throw new ApiException(FamilyErrorCode.USER_ALREADY_IN_FAMILY);
        }

        // 3. QR 코드로 가족 그룹 조회
        FamilyEntity family = familyRepository.findByQrCode(scannedQrCode)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.INVALID_QR_CODE));

        // 4. 만료 시간 검증
        if (family.getQrExpiresAt() != null && family.getQrExpiresAt().isBefore(LocalDateTime.now(clock))) {
            throw new ApiException(FamilyErrorCode.EXPIRED_QR_CODE);
        }

        // 5. 미연동 프로필 매칭 — 이름·생일은 User 엔티티에서 직접 추출
        FamilyProfileEntity profile = familyProfileRepository
                .findByFamilyIdAndNameAndBirthDate(
                        family.getId(), child.getName(), child.getBirthDate())
                .orElseThrow(() -> new ApiException(FamilyErrorCode.PROFILE_NOT_FOUND));

        // 5-1. 프로필이 존재하지만 이미 다른 유저와 연동된 경우
        if (profile.getLinkedUserId() != null) {
            throw new ApiException(FamilyErrorCode.PROFILE_ALREADY_LINKED);
        }

        // 6. 프로필에 유저 ID 연결
        profile.linkUser(childId);

        // 7. 자녀(User)의 family_id 업데이트
        child.updateFamilyId(family.getId());
    }

    /**
     * 4. 가족 QR 재발급 (POST /api/families/qr/reissue)
     */
    @Transactional
    public FamilyQrResponse reissueFamilyQr(UUID userId) {
        UUID familyId = resolveFamilyId(userId);

        FamilyEntity family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.FAMILY_NOT_FOUND));

        String newQrCode = UUID.randomUUID().toString();
        LocalDateTime newExpiresAt = LocalDateTime.now(clock).plusMinutes(5);

        family.updateQrCode(newQrCode, newExpiresAt);
        return new FamilyQrResponse(newQrCode, newExpiresAt);
    }

    /**
     * 5. 가족 구성원 목록 조회 (GET /api/families/members)
     */
    public FamilyMemberListResponse getFamilyMembers(UUID userId) {
        UUID familyId = resolveFamilyId(userId);

        List<FamilyMemberResponse> members = familyProfileRepository.findAllByFamilyId(familyId).stream()
                .map(profile -> {
                    UUID linkedUserId = profile.getLinkedUserId();

                    List<UUID> accountIds = linkedUserId != null
                            ? accountRepository.findAllByUserId(linkedUserId)
                                    .stream()
                                    .map(Account::getId)
                                    .toList()
                            : List.of();

                    return new FamilyMemberResponse(
                            profile.getId(),
                            linkedUserId,
                            profile.getName(),
                            profile.getRole(),
                            accountIds
                    );
                })
                .toList();

        return new FamilyMemberListResponse(members);
    }

    /**
     * 6. 가족 QR 수동 만료 (DELETE /api/families/qr)
     */
    @Transactional
    public void expireFamilyQr(UUID userId) {
        UUID familyId = resolveFamilyId(userId);

        FamilyEntity family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.FAMILY_NOT_FOUND));

        if (family.getQrCode() == null ||
                (family.getQrExpiresAt() != null && family.getQrExpiresAt().isBefore(LocalDateTime.now(clock)))) {
            throw new ApiException(FamilyErrorCode.QR_ALREADY_EXPIRED);
        }

        family.updateQrCode(family.getQrCode(), LocalDateTime.now(clock).minusSeconds(1));
    }

    /**
     * 7. 가족 구성원 정보 수정 (PATCH /api/families/members/{memberId})
     * 미연동 상태인 구성원의 이름과 생년월일을 수정합니다.
     */
    @Transactional
    public void updateFamilyMember(UUID userId, UUID profileId, FamilyMemberUpdateRequest request) {
        UUID familyId = resolveFamilyId(userId);

        FamilyProfileEntity profile = familyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.PROFILE_NOT_FOUND));

        // 1. 권한 검증: 우리 가족 그룹에 속한 프로필이 맞는지 확인
        if (!profile.getFamilyId().equals(familyId)) {
            throw new ApiException(FamilyErrorCode.UNAUTHORIZED_PROFILE_ACCESS);
        }

        // 2. 상태 검증: 이미 실제 유저와 연동이 끝난 프로필은 수정 불가
        if (profile.getLinkedUserId() != null) {
            throw new ApiException(FamilyErrorCode.PROFILE_ALREADY_LINKED);
        }

        // 3. 검증 통과 시 정보 수정 (JPA 더티 체킹으로 자동 반영)
        profile.updateProfileInfo(request.name(), request.birthDate());
    }

    /**
     * 8. 가족 구성원 연결 해제 (DELETE /api/families/members/{memberId})
     * 연동된 유저라면 linkedUserId를 null로 만들어 연동을 해제하고 (Soft Disconnect),
     * 아직 가입하지 않은 미연동 프로필이라면 프로필 자체를 삭제(Hard Delete)합니다.
     */
    @Transactional
    public void removeFamilyMember(UUID userId, UUID profileId) {
        UUID familyId = resolveFamilyId(userId);

        FamilyProfileEntity profile = familyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.PROFILE_NOT_FOUND));

        // 1. 권한 검증: 우리 가족 그룹에 속한 프로필이 맞는지 확인
        if (!profile.getFamilyId().equals(familyId)) {
            throw new ApiException(FamilyErrorCode.UNAUTHORIZED_PROFILE_ACCESS);
        }

        UUID linkedUserId = profile.getLinkedUserId();
        if (linkedUserId != null) {
            // 2-A. 이미 연동된 유저인 경우 → 연동 해제 (Soft Disconnect)
            profile.unlinkUser();

            User linkedUser = userRepository.findById(linkedUserId)
                    .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
            linkedUser.updateFamilyId(null);
        } else {
            // 2-B. 미연동 상태(빈 의자)인 경우 → 프로필 완전 삭제
            familyProfileRepository.delete(profile);
        }
    }
}
