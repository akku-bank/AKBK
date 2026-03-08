package com.akku.backend.domain.family.service;

import com.akku.backend.domain.family.dto.FamilyCreateResponse;
import com.akku.backend.domain.family.dto.FamilyMemberPreRegisterRequest;
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
     */
    @Transactional
    public String getOrGenerateFamilyQr(UUID parentUserId) { // DDL에 맞춰 UUID로 수정
        // TODO: 1. 부모의 family_id로 현재 가족 그룹 찾기
        // TODO: 2. 이미 유효한 QR이 있으면 그거 반환, 없거나 만료됐으면 새로 만들어서 DB 업데이트 후 반환
        return "발급된_QR_문자열";
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
            throw new IllegalStateException("만료된 QR 코드입니다. 부모님께 재발급을 요청하세요.");
        }

        // 3. 미연동 프로필(FamilyProfile) 중 이름과 생년월일이 일치하는 데이터 매칭
        FamilyProfileEntity profile = familyProfileRepository
                .findByFamilyIdAndNameAndBirthDateAndLinkedUserIdIsNull(family.getId(), childName, birthDate)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 사전 등록 정보가 없거나 이미 연동된 구성원입니다."));

        // 4. 프로필에 유저 ID 연결 (연동 완료)
        profile.linkUser(childId);

        // TODO: 3. 검증 통과! 자녀(User) 정보를 DB에서 조회
        // TODO: 4. 자녀의 family_id를 찾은 family.getId()로 업데이트 (가족 합류 완료!)
    }

    // =========================================================================
    // 아래는 추후 채워나갈 나머지 API들의 빈 뼈대
    // =========================================================================

    @Transactional
    public void registerFamilyMember() { /* TODO: 가족 구성원 직접 등록 로직 */ }

    @Transactional
    public void expireFamilyQr() { /* TODO: QR 수동 만료 로직 (qrExpiresAt을 과거 시간으로 변경 등) */ }

    @Transactional
    public void reissueFamilyQr() { /* TODO: 기존 QR 무시하고 새 QR 강제 발급 로직 */ }

    public void getFamilyMembers() { /* TODO: 가족 구성원 목록 조회 로직 (읽기 전용이므로 @Transactional 생략 가능) */ }

    @Transactional
    public void updateFamilyMember() { /* TODO: 자녀 정보(이름 등) 수정 로직 */ }

    @Transactional
    public void removeFamilyMember() { /* TODO: 자녀의 family_id를 null로 만들어서 연결 해제하는 로직 */ }
}