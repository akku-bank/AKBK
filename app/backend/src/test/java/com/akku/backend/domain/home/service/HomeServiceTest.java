package com.akku.backend.domain.home.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.avatar.entity.Item;
import com.akku.backend.domain.avatar.entity.UserItem;
import com.akku.backend.domain.avatar.repository.UserItemRepository;
import com.akku.backend.domain.bank.service.AccountService;
import com.akku.backend.domain.family.entity.FamilyProfileEntity;
import com.akku.backend.domain.family.repository.FamilyProfileRepository;
import com.akku.backend.domain.report.entity.WeeklyReport;
import com.akku.backend.domain.report.repository.WeeklyReportRepository;
import com.akku.backend.domain.home.dto.ChildHomeResponse;
import com.akku.backend.domain.home.dto.ParentHomeResponse;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @InjectMocks
    private HomeService homeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserItemRepository userItemRepository;

    @Mock
    private FamilyProfileRepository familyProfileRepository;

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @Mock
    private AccountService accountService;

    // =====================================================================================
    // 1. getChildHome — 자녀 홈 화면 데이터 조회
    // =====================================================================================

    @Nested
    @DisplayName("자녀 홈 화면 데이터 조회")
    class GetChildHomeTests {

        @Test
        @DisplayName("17. 성공 - 장착 아이템이 있을 때: avatar.equippedItems()가 채워지고 level은 user.level과 일치")
        void getChildHome_WithEquippedItems_ReturnsCorrectLevelAndItems() {
            // given
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            int userLevel = 7;

            int userScore = 75;

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(userLevel);
            given(mockUser.getScore()).willReturn(userScore);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            Item mockItem = mock(Item.class);
            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getCategory()).willReturn("CLOTHES");
            given(mockItem.getResourceUrl()).willReturn("https://cdn.example.com/clothes.png");

            UserItem equippedUserItem = mock(UserItem.class);
            given(equippedUserItem.getItem()).willReturn(mockItem);

            given(userItemRepository.findEquippedItemsByUserId(userId)).willReturn(List.of(equippedUserItem));
            
            given(accountService.getPrimaryAccountBalance(userId)).willReturn(50000L);

            // when
            ChildHomeResponse response = homeService.getChildHome(userId);

            // then
            assertNotNull(response);
            assertEquals(userLevel, response.level());                             // Real 데이터 검증
            assertEquals(userScore, response.score());                             // Real 데이터 검증
            assertFalse(response.avatar().equippedItems().isEmpty());              // 장착 아이템 존재
            assertEquals(itemId, response.avatar().equippedItems().get(0).itemId());
            assertEquals("CLOTHES", response.avatar().equippedItems().get(0).category());
        }

        @Test
        @DisplayName("18. 성공 - 장착 아이템이 없을 때: avatar.equippedItems()가 빈 리스트")
        void getChildHome_NoEquippedItems_ReturnsEmptyItemList() {
            // given
            UUID userId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(3);
            given(mockUser.getScore()).willReturn(40);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userItemRepository.findEquippedItemsByUserId(userId)).willReturn(Collections.emptyList());
            given(accountService.getPrimaryAccountBalance(userId)).willReturn(0L);

            // when
            ChildHomeResponse response = homeService.getChildHome(userId);

            // then
            assertNotNull(response);
            assertTrue(response.avatar().equippedItems().isEmpty());
        }

        @Test
        @DisplayName("19. 실패 - 존재하지 않는 userId: USER_NOT_FOUND 예외 발생")
        void getChildHome_UserNotFound_ThrowsApiException() {
            // given
            UUID unknownUserId = UUID.randomUUID();
            given(userRepository.findById(unknownUserId)).willReturn(Optional.empty());

            // when & then
            ApiException ex = assertThrows(ApiException.class,
                    () -> homeService.getChildHome(unknownUserId));
            assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }
    }

    // =====================================================================================
    // 2. getParentHome — 부모 홈 대시보드 데이터 조회
    // =====================================================================================

    @Nested
    @DisplayName("부모 홈 대시보드 데이터 조회")
    class GetParentHomeTests {

        @Test
        @DisplayName("20. 성공 - CHILD 프로필이 존재할 때: children 리스트 크기가 CHILD 수와 일치")
        void getParentHome_WithChildProfiles_ReturnsCorrectChildList() {
            // given
            UUID userId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();

            User mockParent = mock(User.class);
            given(mockParent.getFamilyId()).willReturn(familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockParent));

            FamilyProfileEntity childProfile1 = mock(FamilyProfileEntity.class);
            given(childProfile1.getRole()).willReturn("CHILD");
            given(childProfile1.getLinkedUserId()).willReturn(UUID.randomUUID());
            given(childProfile1.getName()).willReturn("첫째");

            FamilyProfileEntity childProfile2 = mock(FamilyProfileEntity.class);
            given(childProfile2.getRole()).willReturn("CHILD");
            given(childProfile2.getLinkedUserId()).willReturn(UUID.randomUUID());
            given(childProfile2.getName()).willReturn("둘째");

            given(familyProfileRepository.findAllByFamilyId(familyId))
                    .willReturn(List.of(childProfile1, childProfile2));

            given(accountService.getPrimaryAccountBalance(any(UUID.class))).willReturn(500000L);
            given(weeklyReportRepository.findByIdUserIdAndIdStartDay(any(UUID.class), any(java.time.LocalDate.class)))
                    .willReturn(Collections.emptyList());

            // when
            ParentHomeResponse response = homeService.getParentHome(userId);

            // then
            assertNotNull(response);
            assertEquals(2, response.children().size()); // CHILD 수 일치
            verify(familyProfileRepository).findAllByFamilyId(familyId); // 정확한 familyId로 호출
        }

        @Test
        @DisplayName("21. 성공 - 가족 구성원이 전원 PARENT일 때: children은 빈 리스트 반환")
        void getParentHome_NoChildProfiles_ReturnsEmptyChildList() {
            // given
            UUID userId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();

            User mockParent = mock(User.class);
            given(mockParent.getFamilyId()).willReturn(familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockParent));

            FamilyProfileEntity parentProfile = mock(FamilyProfileEntity.class);
            given(parentProfile.getRole()).willReturn("PARENT"); // CHILD가 없음

            given(familyProfileRepository.findAllByFamilyId(familyId)).willReturn(List.of(parentProfile));
            
            given(accountService.getPrimaryAccountBalance(userId)).willReturn(500000L);

            // when
            ParentHomeResponse response = homeService.getParentHome(userId);

            // then
            assertNotNull(response);
            assertTrue(response.children().isEmpty());
        }

        @Test
        @DisplayName("22. 성공 - 미연동 자녀 (linkedUserId=null): ChildSummaryDto의 childId가 null")
        void getParentHome_UnlinkedChild_HasNullChildId() {
            // given
            UUID userId = UUID.randomUUID();
            UUID familyId = UUID.randomUUID();

            User mockParent = mock(User.class);
            given(mockParent.getFamilyId()).willReturn(familyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockParent));

            FamilyProfileEntity unlinkedChild = mock(FamilyProfileEntity.class);
            given(unlinkedChild.getRole()).willReturn("CHILD");
            given(unlinkedChild.getLinkedUserId()).willReturn(null); // 아직 앱 가입 전
            given(unlinkedChild.getName()).willReturn("미연동 자녀");

            given(familyProfileRepository.findAllByFamilyId(familyId)).willReturn(List.of(unlinkedChild));

            given(accountService.getPrimaryAccountBalance(userId)).willReturn(500000L);

            // when
            ParentHomeResponse response = homeService.getParentHome(userId);

            // then
            assertEquals(1, response.children().size());
            assertNull(response.children().get(0).childId()); // 미연동 자녀의 ID는 null
        }

        @Test
        @DisplayName("23. 실패 - 존재하지 않는 userId: USER_NOT_FOUND 예외 발생")
        void getParentHome_UserNotFound_ThrowsApiException() {
            // given
            UUID unknownUserId = UUID.randomUUID();
            given(userRepository.findById(unknownUserId)).willReturn(Optional.empty());

            // when & then
            ApiException ex = assertThrows(ApiException.class,
                    () -> homeService.getParentHome(unknownUserId));
            assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("24. 성공 - familyProfileRepository는 parent.getFamilyId()로 정확히 호출됨")
        void getParentHome_CallsFamilyRepositoryWithCorrectFamilyId() {
            // given
            UUID userId = UUID.randomUUID();
            UUID expectedFamilyId = UUID.randomUUID();

            User mockParent = mock(User.class);
            given(mockParent.getFamilyId()).willReturn(expectedFamilyId);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockParent));
            given(familyProfileRepository.findAllByFamilyId(expectedFamilyId)).willReturn(Collections.emptyList());

            given(accountService.getPrimaryAccountBalance(userId)).willReturn(500000L);

            // when
            homeService.getParentHome(userId);

            // then — 잘못된 familyId로 호출되면 실패
            verify(familyProfileRepository).findAllByFamilyId(expectedFamilyId);
        }
    }
}
