package com.akku.backend.domain.family.service;

import com.akku.backend.domain.family.dto.*;
import com.akku.backend.domain.family.entity.FamilyEntity;
import com.akku.backend.domain.family.entity.FamilyProfileEntity;
import com.akku.backend.domain.family.exception.FamilyErrorCode;
import com.akku.backend.domain.family.repository.FamilyProfileRepository;
import com.akku.backend.domain.family.repository.FamilyRepository;
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @InjectMocks
    private FamilyService familyService; // 테스트할 진짜 객체 (의존성이 주입됨)

    @Mock
    private FamilyRepository familyRepository; // 가짜(Mock) DB 레포지토리

    @Mock
    private FamilyProfileRepository familyProfileRepository; // 가짜(Mock) DB 레포지토리

    @Test
    @DisplayName("가족 그룹 생성 - 성공 시 새로운 가족 ID를 반환한다")
    void createFamilyGroup_Success() {
        // given (준비: 가짜 데이터와 행동 설정)
        UUID parentId = UUID.randomUUID();
        UUID mockFamilyId = UUID.randomUUID();

        FamilyEntity mockFamily = mock(FamilyEntity.class);
        given(mockFamily.getId()).willReturn(mockFamilyId);
        given(familyRepository.save(any(FamilyEntity.class))).willReturn(mockFamily);

        // when (실행)
        FamilyCreateResponse response = familyService.createFamilyGroup(parentId);

        // then (검증)
        assertNotNull(response);
        assertEquals(mockFamilyId, response.familyId());
        verify(familyRepository, times(1)).save(any(FamilyEntity.class)); // save가 1번 호출되었는지 검증
    }

    @Test
    @DisplayName("가족 그룹 합류 - 만료된 QR 코드일 경우 예외가 발생한다")
    void joinFamilyGroup_WhenQrExpired_ThrowsException() {
        // given
        UUID childId = UUID.randomUUID();
        String scannedQrCode = "fake-qr-code";

        FamilyEntity mockFamily = mock(FamilyEntity.class);
        given(mockFamily.getQrExpiresAt()).willReturn(LocalDateTime.now().minusMinutes(10)); // 10분 전(과거)으로 세팅

        given(familyRepository.findByQrCode(scannedQrCode)).willReturn(Optional.of(mockFamily));

        // when & then
        ApiException exception = assertThrows(ApiException.class, () ->
                familyService.joinFamilyGroup(childId, scannedQrCode, "김싸피", LocalDate.of(2010, 1, 1))
        );

        // 우리가 만든 커스텀 에러 코드가 정확히 터지는지 검증
        assertEquals(FamilyErrorCode.EXPIRED_QR_CODE, exception.getErrorCode());
    }

    @Test
    @DisplayName("가족 그룹 합류 - 정상적인 QR과 프로필 매칭 시 연동(linkUser)에 성공한다")
    void joinFamilyGroup_Success() {
        // given
        UUID childId = UUID.randomUUID();
        String scannedQrCode = "valid-qr-code";
        UUID familyId = UUID.randomUUID();
        String childName = "김싸피";
        LocalDate birthDate = LocalDate.of(2010, 1, 1);

        // 가짜 가족 (QR 유효함)
        FamilyEntity mockFamily = mock(FamilyEntity.class);
        given(mockFamily.getId()).willReturn(familyId);
        given(mockFamily.getQrExpiresAt()).willReturn(LocalDateTime.now().plusMinutes(10)); // 미래 시간
        given(familyRepository.findByQrCode(scannedQrCode)).willReturn(Optional.of(mockFamily));

        // 가짜 프로필 (매칭됨)
        FamilyProfileEntity mockProfile = mock(FamilyProfileEntity.class);
        given(familyProfileRepository.findByFamilyIdAndNameAndBirthDateAndLinkedUserIdIsNull(
                familyId, childName, birthDate)).willReturn(Optional.of(mockProfile));

        // when
        familyService.joinFamilyGroup(childId, scannedQrCode, childName, birthDate);

        // then
        verify(mockProfile, times(1)).linkUser(childId); // 프로필에 자녀 ID가 정상적으로 연동되었는지 확인
    }

    @Test
    @DisplayName("가족 구성원 정보 수정 - 이미 연동된 유저의 정보를 수정하려 하면 예외가 발생한다")
    void updateFamilyMember_WhenAlreadyLinked_ThrowsException() {
        // given
        UUID familyId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        FamilyMemberUpdateRequest request = new FamilyMemberUpdateRequest("새이름", LocalDate.now());

        FamilyProfileEntity mockProfile = mock(FamilyProfileEntity.class);
        given(mockProfile.getFamilyId()).willReturn(familyId); // 우리 가족은 맞음
        given(mockProfile.getLinkedUserId()).willReturn(UUID.randomUUID()); // 🚨 이미 연동된 유저 ID가 존재함

        given(familyProfileRepository.findById(profileId)).willReturn(Optional.of(mockProfile));

        // when & then
        ApiException exception = assertThrows(ApiException.class, () ->
                familyService.updateFamilyMember(familyId, profileId, request)
        );
        assertEquals(FamilyErrorCode.PROFILE_ALREADY_LINKED, exception.getErrorCode());
    }

    @Test
    @DisplayName("가족 구성원 연결 해제 - 미연동 상태인 경우 프로필 자체를 완전 삭제(Hard Delete)한다")
    void removeFamilyMember_WhenNotLinked_DeletesProfile() {
        // given
        UUID familyId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        FamilyProfileEntity mockProfile = mock(FamilyProfileEntity.class);
        given(mockProfile.getFamilyId()).willReturn(familyId);
        given(mockProfile.getLinkedUserId()).willReturn(null); // 연동 안 됨 (빈 의자)

        given(familyProfileRepository.findById(profileId)).willReturn(Optional.of(mockProfile));

        // when
        familyService.removeFamilyMember(familyId, profileId);

        // then
        verify(familyProfileRepository, times(1)).delete(mockProfile); // delete 쿼리가 호출되었는지 검증
        verify(mockProfile, never()).unlinkUser(); // unlink 로직은 타면 안 됨
    }
}