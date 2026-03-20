package com.akku.backend.domain.family.service;

import com.akku.backend.domain.bank.entity.Account;
import com.akku.backend.domain.bank.repository.AccountRepository;
import com.akku.backend.domain.family.dto.*;
import com.akku.backend.domain.family.entity.FamilyEntity;
import com.akku.backend.domain.family.entity.FamilyProfileEntity;
import com.akku.backend.domain.family.exception.FamilyErrorCode;
import com.akku.backend.domain.family.repository.FamilyProfileRepository;
import com.akku.backend.domain.family.repository.FamilyRepository;
import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @InjectMocks
    private FamilyService familyService;

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyProfileRepository familyProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("가족 그룹 생성 및 등록")
    class RegistrationTests {
        @Test
        @DisplayName("1. 가족 그룹 생성 - 성공 (미소속 부모)")
        void createFamilyGroup_Success() {
            UUID parentId = UUID.randomUUID();

            User mockParent = mock(User.class);
            given(mockParent.getFamilyId()).willReturn(null);
            given(userRepository.findById(parentId)).willReturn(Optional.of(mockParent));

            FamilyEntity mockFamily = mock(FamilyEntity.class);
            given(mockFamily.getId()).willReturn(UUID.randomUUID());
            given(familyRepository.save(any(FamilyEntity.class))).willReturn(mockFamily);

            FamilyCreateResponse response = familyService.createFamilyGroup(parentId);

            assertNotNull(response.familyId());
            verify(mockParent).updateFamilyId(mockFamily.getId());
        }

        @Test
        @DisplayName("1-B. 가족 그룹 생성 - 실패 (이미 다른 가족 그룹 소속)")
        void createFamilyGroup_Fail_AlreadyInFamily() {
            UUID parentId = UUID.randomUUID();

            User mockParent = mock(User.class);
            given(mockParent.getFamilyId()).willReturn(UUID.randomUUID());
            given(userRepository.findById(parentId)).willReturn(Optional.of(mockParent));

            ApiException ex = assertThrows(ApiException.class,
                    () -> familyService.createFamilyGroup(parentId));

            assertEquals(FamilyErrorCode.USER_ALREADY_IN_FAMILY, ex.getErrorCode());
            verify(familyRepository, never()).save(any(FamilyEntity.class));
        }

        @Test
        @DisplayName("1-1. 가족 구성원 사전 등록 - 성공")
        void preRegisterFamilyMember_Success() {
            UUID userId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();
            FamilyMemberPreRegisterRequest request = new FamilyMemberPreRegisterRequest("자녀1", "CHILD", LocalDate.now());

            User mockUser = mock(User.class);
            given(mockUser.getFamilyId()).willReturn(familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            familyService.preRegisterFamilyMember(userId, request);

            verify(familyProfileRepository).save(any(FamilyProfileEntity.class));
        }
    }

    @Nested
    @DisplayName("QR 코드 관리 로직")
    class QrManagementTests {
        @Test
        @DisplayName("2. QR 조회 - 유효한 QR이 없으면 새로 생성")
        void getOrGenerateFamilyQr_New() {
            UUID userId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getFamilyId()).willReturn(familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            FamilyEntity family = mock(FamilyEntity.class);
            given(family.getQrCode()).willReturn(null);
            given(familyRepository.findById(familyId)).willReturn(Optional.of(family));

            FamilyQrResponse response = familyService.getOrGenerateFamilyQr(userId);

            assertNotNull(response.qrCode());
            verify(family).updateQrCode(anyString(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("4. QR 강제 재발급 - 기존 상관없이 무조건 새로 생성")
        void reissueFamilyQr_Success() {
            UUID userId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getFamilyId()).willReturn(familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            FamilyEntity family = mock(FamilyEntity.class);
            given(familyRepository.findById(familyId)).willReturn(Optional.of(family));

            FamilyQrResponse response = familyService.reissueFamilyQr(userId);

            assertNotNull(response.qrCode());
            verify(family).updateQrCode(anyString(), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("가족 합류 및 목록 조회")
    class MemberInteractionTests {
        @Test
        @DisplayName("3. 가족 합류 - 성공 (미소속 자녀 + 정상 QR + 프로필 매칭)")
        void joinFamilyGroup_Success() {
            UUID childId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();
            String qr = "valid-qr";

            User mockChild = mock(User.class);
            given(mockChild.getFamilyId()).willReturn(null);
            given(userRepository.findById(childId)).willReturn(Optional.of(mockChild));

            FamilyEntity family = mock(FamilyEntity.class);
            given(family.getId()).willReturn(familyId);
            given(family.getQrExpiresAt()).willReturn(LocalDateTime.now().plusMinutes(10));
            given(familyRepository.findByQrCode(qr)).willReturn(Optional.of(family));

            FamilyProfileEntity profile = mock(FamilyProfileEntity.class);
            given(profile.getLinkedUserId()).willReturn(null); // 미연동 상태
            given(familyProfileRepository.findByFamilyIdAndNameAndBirthDate(any(), any(), any()))
                    .willReturn(Optional.of(profile));

            familyService.joinFamilyGroup(childId, qr);

            verify(profile).linkUser(childId);
            verify(mockChild).updateFamilyId(familyId);
        }

        @Test
        @DisplayName("3-B. 가족 합류 - 실패 (이미 가족 그룹에 소속된 자녀)")
        void joinFamilyGroup_Fail_AlreadyInFamily() {
            UUID childId = UUID.randomUUID();
            String qr = "valid-qr";

            User mockChild = mock(User.class);
            given(mockChild.getFamilyId()).willReturn(UUID.randomUUID());
            given(userRepository.findById(childId)).willReturn(Optional.of(mockChild));

            ApiException ex = assertThrows(ApiException.class,
                    () -> familyService.joinFamilyGroup(childId, qr));

            assertEquals(FamilyErrorCode.USER_ALREADY_IN_FAMILY, ex.getErrorCode());
            verify(familyRepository, never()).findByQrCode(anyString());
            verify(familyProfileRepository, never()).findByFamilyIdAndNameAndBirthDate(any(), any(), any());
        }

        @Test
        @DisplayName("5. 가족 구성원 목록 조회 - 리스트 반환 확인")
        void getFamilyMembers_Success() {
            UUID userId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();
            UUID linkedUserId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getFamilyId()).willReturn(familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            FamilyProfileEntity profile = mock(FamilyProfileEntity.class);
            given(profile.getLinkedUserId()).willReturn(linkedUserId);
            given(familyProfileRepository.findAllByFamilyId(familyId)).willReturn(List.of(profile));

            Account mockAccount = mock(Account.class);
            given(mockAccount.getId()).willReturn(UUID.randomUUID());
            given(accountRepository.findAllByUserId(linkedUserId)).willReturn(List.of(mockAccount));

            FamilyMemberListResponse response = familyService.getFamilyMembers(userId);

            assertFalse(response.members().isEmpty());
            assertNotNull(response.members().get(0).accountId());
        }
    }

    @Nested
    @DisplayName("수정 및 삭제 권한/상태 검증")
    class AuthorityTests {
        @Test
        @DisplayName("7. 정보 수정 - 이미 연동된 유저는 수정 불가 (ApiException)")
        void updateFamilyMember_AlreadyLinked() {
            UUID userId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getFamilyId()).willReturn(familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            FamilyProfileEntity profile = mock(FamilyProfileEntity.class);
            given(profile.getFamilyId()).willReturn(familyId);
            given(profile.getLinkedUserId()).willReturn(UUID.randomUUID());
            given(familyProfileRepository.findById(any())).willReturn(Optional.of(profile));

            ApiException ex = assertThrows(ApiException.class, () ->
                    familyService.updateFamilyMember(userId, UUID.randomUUID(), new FamilyMemberUpdateRequest("이름", LocalDate.now())));

            assertEquals(FamilyErrorCode.PROFILE_ALREADY_LINKED, ex.getErrorCode());
        }

        @Test
        @DisplayName("8-A. 연결 해제 - 연동된 유저는 Soft Disconnect")
        void removeFamilyMember_Soft() {
            UUID userId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();
            UUID linkedUserId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getFamilyId()).willReturn(familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            FamilyProfileEntity profile = mock(FamilyProfileEntity.class);
            given(profile.getFamilyId()).willReturn(familyId);
            given(profile.getLinkedUserId()).willReturn(linkedUserId);
            given(familyProfileRepository.findById(any())).willReturn(Optional.of(profile));

            User linkedUser = mock(User.class);
            given(userRepository.findById(linkedUserId)).willReturn(Optional.of(linkedUser));

            familyService.removeFamilyMember(userId, UUID.randomUUID());

            verify(profile).unlinkUser();
            verify(linkedUser).updateFamilyId(null);
            verify(familyProfileRepository, never()).delete(any());
        }
    }
}
