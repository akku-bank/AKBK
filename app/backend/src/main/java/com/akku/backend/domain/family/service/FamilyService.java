package com.akku.backend.domain.family.service;

import com.akku.backend.domain.family.dto.*;
import com.akku.backend.domain.family.entity.FamilyEntity;
import com.akku.backend.domain.family.entity.FamilyProfileEntity;
import com.akku.backend.domain.family.repository.FamilyProfileRepository;
import com.akku.backend.domain.family.repository.FamilyRepository;
import com.akku.backend.domain.family.exception.FamilyErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyProfileRepository familyProfileRepository; // 미연동 프로필 관리를 위해 주입
    // private final UserRepository userRepository; // 자녀 합류 시 유저 테이블 업데이트용

    /**
     * 1. 가족 그룹 생성 (최초 빈 그룹 생성)
     *
     * @param parentId 가족 그룹을 생성하는 부모의 ID
     * @return 생성된 가족 그룹의 고유 ID
     */
    @Transactional
    public FamilyCreateResponse createFamilyGroup(UUID parentId) {

        // 1. 빈 가족 그룹 엔티티 생성 (QR 코드는 별도 API로 발급)
        FamilyEntity newFamily = FamilyEntity.builder().build();

        // 2. DB 저장 (UUID 및 createdAt 자동 생성)
        FamilyEntity savedFamily = familyRepository.save(newFamily);

        // TODO: 3. 부모(User) 테이블의 family_id를 방금 생성한 savedFamily.getId()로 업데이트
        // UserEntity parent = userRepository.findById(parentId).orElseThrow(...);
        // parent.updateFamilyId(savedFamily.getId());

        return new FamilyCreateResponse(savedFamily.getId());
    }

    /**
     * 1-1. 가족 구성원 사전 등록 (미연동 프로필 생성)
     * 부모가 자녀의 이름과 생년월일을 미리 등록하여 가입 대기 상태의 프로필을 생성
     */
    @Transactional
    public void preRegisterFamilyMember(UUID familyId, FamilyMemberPreRegisterRequest request) {
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
    public FamilyQrResponse getOrGenerateFamilyQr(UUID familyId) { // DDL에 맞춰 UUID로 수정
        // 1. 가족 그룹 조회
        FamilyEntity family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.FAMILY_NOT_FOUND));

        // 2. 유효한 QR이 있는지 확인 (문자열이 있고, 만료 시간이 현재보다 미래인 경우)
        if (family.getQrCode() != null &&
                family.getQrExpiresAt() != null &&
                family.getQrExpiresAt().isAfter(LocalDateTime.now())) {

            return new FamilyQrResponse(family.getQrCode(), family.getQrExpiresAt());
        }

        // 3. QR이 없거나 만료되었다면 새로 생성 (UUID 활용)
        String newQrCode = UUID.randomUUID().toString();
        LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(5); // 유효기간 5분 설정

        family.updateQrCode(newQrCode, newExpiresAt); // 엔티티 메서드 활용

        return new FamilyQrResponse(newQrCode, newExpiresAt);
    }

    /**
     * 3. 가족 그룹 합류 (QR 스캔 + 이름/생일 기반 자동 연동)
     */
    @Transactional
    public void joinFamilyGroup(UUID childId, String scannedQrCode, String childName, LocalDate birthDate) {
        // 1. Repository를 이용해 QR 코드로 가족 찾기
        FamilyEntity family = familyRepository.findByQrCode(scannedQrCode)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.INVALID_QR_CODE));

        // 2. 만료 시간 검증 (서비스 단에서 자바 로직으로 처리!)
        if (family.getQrExpiresAt() != null && family.getQrExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(FamilyErrorCode.EXPIRED_QR_CODE);
        }

        // 3. 미연동 프로필(FamilyProfile) 중 이름과 생년월일이 일치하는 데이터 매칭
        FamilyProfileEntity profile = familyProfileRepository
                .findByFamilyIdAndNameAndBirthDateAndLinkedUserIdIsNull(family.getId(), childName, birthDate)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.PROFILE_ALREADY_LINKED));
        // 4. 프로필에 유저 ID 연결 (연동 완료)
        profile.linkUser(childId);

        // TODO: 3. 검증 통과! 자녀(User) 정보를 DB에서 조회
        // TODO: 4. 자녀의 family_id를 찾은 family.getId()로 업데이트 (가족 합류 완료!)
    }

    /**
     * 4. 가족 QR 재발급 (POST /api/families/qr/reissue)
     * 🚨 누락되었던 강제 재발급 로직 추가
     */
    @Transactional
    public FamilyQrResponse reissueFamilyQr(UUID familyId) {
        FamilyEntity family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.FAMILY_NOT_FOUND));

        String newQrCode = UUID.randomUUID().toString();
        LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(5);

        family.updateQrCode(newQrCode, newExpiresAt);
        return new FamilyQrResponse(newQrCode, newExpiresAt);
    }

    /**
     * 5. 가족 구성원 목록 조회 (GET /api/families/members)
     */
    public FamilyMemberListResponse getFamilyMembers(UUID familyId) {
        List<FamilyMemberResponse> members = familyProfileRepository.findAllByFamilyId(familyId).stream()
                .map(profile -> {
                    UUID userId = profile.getLinkedUserId();
                    UUID accountId = null;

                    // 1. 연동이 완료된 자녀라면 계좌 ID를 찾아옴
                    if (userId != null) {
                        // TODO: AccountRepository가 완성되면 아래 주석을 풀고 연결
                        // accountId = accountRepository.findByUserId(userId)
                        //         .map(AccountEntity::getId)
                        //         .orElse(null);

                        // 임시 더미 데이터 (테스트용)
                        accountId = UUID.randomUUID();
                    }

                    // 2. 응답 DTO 조립
                    return new FamilyMemberResponse(
                            profile.getId(),
                            userId,
                            profile.getName(),
                            profile.getRole(),
                            accountId
                    );
                })
                .toList();

        return new FamilyMemberListResponse(members);
    }

    /**
     * 6. 가족 QR 수동 만료 (DELETE /api/families/qr)
     * 발급된 QR 코드를 즉시 만료 처리하여 보안을 강화합니다.
     */
    @Transactional
    public void expireFamilyQr(UUID familyId) {
        FamilyEntity family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.FAMILY_NOT_FOUND));

        // 이미 만료되었거나 QR이 없는 경우 예외 처리
        if (family.getQrCode() == null ||
                (family.getQrExpiresAt() != null && family.getQrExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new ApiException(FamilyErrorCode.QR_ALREADY_EXPIRED);
        }

        // 만료 시간을 현재 시간보다 1초 과거로 설정하여 즉시 만료 처리
        family.updateQrCode(family.getQrCode(), LocalDateTime.now().minusSeconds(1));
    }

    /**
     * 7. 가족 구성원 정보 수정 (PATCH /api/families/members/{memberId})
     * 미연동 상태인 구성원의 이름과 생년월일을 수정합니다.
     */
    @Transactional
    public void updateFamilyMember(UUID familyId, UUID profileId, FamilyMemberUpdateRequest request) {
        FamilyProfileEntity profile = familyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.PROFILE_NOT_FOUND));

        // 1. 권한 검증: 우리 가족 그룹에 속한 프로필이 맞는지 확인
        if (!profile.getFamilyId().equals(familyId)) {
            throw new ApiException(FamilyErrorCode.UNAUTHORIZED_PROFILE_ACCESS);
        }

        // 2. 상태 검증 (방안 A): 이미 실제 유저와 연동이 끝난 프로필은 수정 불가
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
    public void removeFamilyMember(UUID familyId, UUID profileId) {
        FamilyProfileEntity profile = familyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ApiException(FamilyErrorCode.PROFILE_NOT_FOUND));

        // 1. 권한 검증: 우리 가족 그룹에 속한 프로필이 맞는지 확인
        if (!profile.getFamilyId().equals(familyId)) {
            throw new ApiException(FamilyErrorCode.UNAUTHORIZED_PROFILE_ACCESS);
        }

        UUID userId = profile.getLinkedUserId();
        if (userId != null) {
            // 2-A. 이미 연동된 유저인 경우 -> 연동 해제 (Soft Disconnect)
            profile.unlinkUser();

            // TODO: User 도메인이 완성되면 아래 주석을 풀고 실제 유저의 family_id도 null로 업데이트
            // UserEntity user = userRepository.findById(userId)
            //         .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
            // user.updateFamilyId(null);
        } else {
            // 2-B. 미연동 상태(빈 의자)인 경우 -> 프로필 완전 삭제
            // 가입도 안 한 상태에서 지우는 것이므로 DB에서 날리는 것이 깔끔합니다.
            familyProfileRepository.delete(profile);
        }
    }



}