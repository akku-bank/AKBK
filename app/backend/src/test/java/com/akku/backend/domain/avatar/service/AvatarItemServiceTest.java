package com.akku.backend.domain.avatar.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.avatar.dto.AvatarEquipmentUpdateRequest;
import com.akku.backend.domain.avatar.dto.AvatarItemListResponse;
import com.akku.backend.domain.avatar.entity.Item;
import com.akku.backend.domain.avatar.entity.UserItem;
import com.akku.backend.domain.avatar.exception.AvatarErrorCode;
import com.akku.backend.domain.avatar.repository.ItemRepository;
import com.akku.backend.domain.avatar.repository.UserItemRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvatarItemServiceTest {

    @InjectMocks
    private AvatarItemService avatarItemService;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserItemRepository userItemRepository;

    @Mock
    private UserRepository userRepository;

    // =====================================================================================
    // 1. getAvatarItems — 아이템 도감(전체 목록) 조회
    // =====================================================================================

    @Nested
    @DisplayName("아이템 도감 조회 - 카테고리 필터 분기")
    class GetAvatarItemsTests {

        @Test
        @DisplayName("1. 성공 - category=null이면 전체 아이템 조회 (findAll)")
        void getAvatarItems_WithNullCategory_CallsFindAll() {
            // given
            UUID userId = UUID.randomUUID();
            User mockUser = mock(User.class);
            // items 목록이 빈 리스트이므로 stream 내부 람다가 실행되지 않아 getLevel()은 불필요 → 제거
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(itemRepository.findAll()).willReturn(Collections.emptyList());
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList());

            // when
            AvatarItemListResponse response = avatarItemService.getAvatarItems(userId, null);

            // then
            assertNotNull(response);
            verify(itemRepository).findAll();
            verify(itemRepository, never()).findByCategory(anyString());
        }

        @Test
        @DisplayName("2. 성공 - category=\"CLOTHES\"이면 카테고리 필터 조회 (findByCategory)")
        void getAvatarItems_WithCategory_CallsFindByCategory() {
            // given
            UUID userId = UUID.randomUUID();
            User mockUser = mock(User.class);
            // items 목록이 빈 리스트이므로 stream 내부 람다가 실행되지 않아 getLevel()은 불필요 → 제거
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(itemRepository.findByCategory("CLOTHES")).willReturn(Collections.emptyList());
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList());

            // when
            AvatarItemListResponse response = avatarItemService.getAvatarItems(userId, "CLOTHES");

            // then
            assertNotNull(response);
            verify(itemRepository).findByCategory("CLOTHES");
            verify(itemRepository, never()).findAll();
        }

        @Test
        @DisplayName("3. 성공 - category가 빈 문자열이면 전체 조회로 처리 (findAll)")
        void getAvatarItems_WithBlankCategory_CallsFindAll() {
            // given
            UUID userId = UUID.randomUUID();
            User mockUser = mock(User.class);
            // items 목록이 빈 리스트이므로 stream 내부 람다가 실행되지 않아 getLevel()은 불필요 → 제거
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(itemRepository.findAll()).willReturn(Collections.emptyList());
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList());

            // when
            AvatarItemListResponse response = avatarItemService.getAvatarItems(userId, "   ");

            // then
            assertNotNull(response);
            verify(itemRepository).findAll();
            verify(itemRepository, never()).findByCategory(anyString());
        }

        @Test
        @DisplayName("4. 실패 - 존재하지 않는 userId로 조회 시 USER_NOT_FOUND 예외 발생")
        void getAvatarItems_UserNotFound_ThrowsApiException() {
            // given
            UUID unknownUserId = UUID.randomUUID();
            given(userRepository.findById(unknownUserId)).willReturn(Optional.empty());

            // when & then
            ApiException ex = assertThrows(ApiException.class,
                    () -> avatarItemService.getAvatarItems(unknownUserId, null));
            assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }
    }

    // =====================================================================================
    // 1-2. getAvatarItems — isLevelLocked / isOwned 판정 경계 조건
    // =====================================================================================

    @Nested
    @DisplayName("아이템 도감 조회 - 레벨 잠금 및 소유 판정 경계 조건")
    class ItemOwnershipLogicTests {

        @Test
        @DisplayName("5. 경계값 - requiredLevel == user.level이면 잠금 해제 (isLevelLocked=false, isOwned=true)")
        void getAvatarItems_RequiredLevelEqualsUserLevel_IsNotLocked() {
            // given
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(5); // 유저 레벨 = 5

            Item mockItem = mock(Item.class);
            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getRequiredLevel()).willReturn(5); // 아이템 필요 레벨 = 5 (경계값)
            given(mockItem.getCategory()).willReturn("CLOTHES");
            given(mockItem.getName()).willReturn("기본 셔츠");
            given(mockItem.getResourceUrl()).willReturn("https://example.com/shirt.png");

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(itemRepository.findAll()).willReturn(List.of(mockItem));
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList());

            // when
            AvatarItemListResponse response = avatarItemService.getAvatarItems(userId, null);

            // then
            assertFalse(response.items().get(0).isLevelLocked()); // 레벨 잠금 해제
            assertTrue(response.items().get(0).isOwned());        // 소유 가능
        }

        @Test
        @DisplayName("6. 레벨 부족 - requiredLevel > user.level이면 잠금 (isLevelLocked=true, isOwned=false)")
        void getAvatarItems_RequiredLevelAboveUserLevel_IsLockedAndNotOwned() {
            // given
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(3); // 유저 레벨 = 3

            Item mockItem = mock(Item.class);
            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getRequiredLevel()).willReturn(5); // 아이템 필요 레벨 = 5 (부족)
            given(mockItem.getCategory()).willReturn("CLOTHES");
            given(mockItem.getName()).willReturn("고급 셔츠");
            given(mockItem.getResourceUrl()).willReturn("https://example.com/premium-shirt.png");

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(itemRepository.findAll()).willReturn(List.of(mockItem));
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList());

            // when
            AvatarItemListResponse response = avatarItemService.getAvatarItems(userId, null);

            // then
            assertTrue(response.items().get(0).isLevelLocked());   // 레벨 잠금 활성
            assertFalse(response.items().get(0).isOwned());         // 소유 불가
        }

        @Test
        @DisplayName("7. 핵심 경계값 - 레벨 부족이지만 뽑기로 획득한 아이템은 isOwned=true")
        void getAvatarItems_LevelLockedButOwnedByGacha_IsOwnedTrue() {
            // given
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(1); // 유저 레벨 = 1 (매우 낮음)

            Item mockItem = mock(Item.class);
            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getRequiredLevel()).willReturn(10); // 아이템 필요 레벨 = 10 (레벨 매우 부족)
            given(mockItem.getCategory()).willReturn("RARE");
            given(mockItem.getName()).willReturn("희귀 코스튬");
            given(mockItem.getResourceUrl()).willReturn("https://example.com/rare.png");

            // 뽑기로 획득한 UserItem 스텁 — Item.getId()가 itemId와 일치
            UserItem gachaUserItem = mock(UserItem.class);
            given(gachaUserItem.getItem()).willReturn(mockItem);

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(itemRepository.findAll()).willReturn(List.of(mockItem));
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(List.of(gachaUserItem));

            // when
            AvatarItemListResponse response = avatarItemService.getAvatarItems(userId, null);

            // then
            assertTrue(response.items().get(0).isLevelLocked());  // 여전히 레벨 잠금
            assertTrue(response.items().get(0).isOwned());         // 하지만 소유 처리
        }

        @Test
        @DisplayName("8. 빈 인벤토리 - 아이템 목록이 비어 있으면 빈 리스트 반환")
        void getAvatarItems_NoItems_ReturnsEmptyList() {
            // given
            UUID userId = UUID.randomUUID();
            User mockUser = mock(User.class);
            // items 목록이 빈 리스트이므로 stream 내부 람다가 실행되지 않아 getLevel()은 불필요 → 제거
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(itemRepository.findAll()).willReturn(Collections.emptyList());
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList());

            // when
            AvatarItemListResponse response = avatarItemService.getAvatarItems(userId, null);

            // then
            assertNotNull(response);
            assertTrue(response.items().isEmpty());
        }
    }

    // =====================================================================================
    // 2. updateEquipment — 아이템 장착/변경 (일괄 업데이트)
    // =====================================================================================

    @Nested
    @DisplayName("아이템 장착 업데이트")
    class UpdateEquipmentTests {

        @Test
        @DisplayName("9. 성공 - 기존 UserItem이 있는 아이템: equip() 호출, save() 미호출 (Update 경로)")
        void updateEquipment_ExistingUserItem_CallsEquip() {
            // given
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(10);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            Item mockItem = mock(Item.class);
            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getRequiredLevel()).willReturn(1); // 레벨 충족

            UserItem existingUserItem = mock(UserItem.class);
            given(existingUserItem.getItem()).willReturn(mockItem);

            given(itemRepository.findAllById(List.of(itemId))).willReturn(List.of(mockItem));
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(List.of(existingUserItem));
            given(userItemRepository.findEquippedItemsByUserId(userId)).willReturn(Collections.emptyList());
            given(userItemRepository.findByUserIdAndItemIdIn(userId, List.of(itemId))).willReturn(List.of(existingUserItem));

            AvatarEquipmentUpdateRequest request = new AvatarEquipmentUpdateRequest(List.of(itemId));

            // when
            avatarItemService.updateEquipment(userId, request);

            // then
            verify(existingUserItem).equip();
            verify(userItemRepository, never()).save(any(UserItem.class));
        }

        @Test
        @DisplayName("10. 성공 - UserItem 미존재 아이템: save() 1회 호출 (Insert 경로 — 레벨업 후 첫 착용)")
        void updateEquipment_NewLevelUpItem_CallsSave() {
            // given
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(10);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            Item mockItem = mock(Item.class);
            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getRequiredLevel()).willReturn(5); // 레벨 충족

            given(itemRepository.findAllById(List.of(itemId))).willReturn(List.of(mockItem));
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList()); // 보유 이력 없음
            given(userItemRepository.findEquippedItemsByUserId(userId)).willReturn(Collections.emptyList());
            given(userItemRepository.findByUserIdAndItemIdIn(userId, List.of(itemId))).willReturn(Collections.emptyList()); // user_items에 없음

            AvatarEquipmentUpdateRequest request = new AvatarEquipmentUpdateRequest(List.of(itemId));

            // when
            avatarItemService.updateEquipment(userId, request);

            // then
            verify(userItemRepository).save(any(UserItem.class));
        }

        @Test
        @DisplayName("11. 성공 - 이전 장착 아이템 전부 해제 (unequip() N회 호출 검증)")
        void updateEquipment_UnequipsAllPreviousItems() {
            // given
            UUID userId = UUID.randomUUID();
            UUID newItemId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(10);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            Item newItem = mock(Item.class);
            given(newItem.getId()).willReturn(newItemId);
            given(newItem.getRequiredLevel()).willReturn(1);

            // 기존에 장착 중이던 아이템 3개
            UserItem equipped1 = mock(UserItem.class);
            UserItem equipped2 = mock(UserItem.class);
            UserItem equipped3 = mock(UserItem.class);

            UserItem newUserItem = mock(UserItem.class);
            given(newUserItem.getItem()).willReturn(newItem);

            given(itemRepository.findAllById(List.of(newItemId))).willReturn(List.of(newItem));
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList());
            given(userItemRepository.findEquippedItemsByUserId(userId)).willReturn(List.of(equipped1, equipped2, equipped3));
            given(userItemRepository.findByUserIdAndItemIdIn(userId, List.of(newItemId))).willReturn(List.of(newUserItem));

            AvatarEquipmentUpdateRequest request = new AvatarEquipmentUpdateRequest(List.of(newItemId));

            // when
            avatarItemService.updateEquipment(userId, request);

            // then — 기존 장착 아이템 3개 모두 unequip() 호출
            verify(equipped1).unequip();
            verify(equipped2).unequip();
            verify(equipped3).unequip();
        }

        @Test
        @DisplayName("12. 실패 - 존재하지 않는 userId: USER_NOT_FOUND 예외 발생")
        void updateEquipment_UserNotFound_ThrowsApiException() {
            // given
            UUID unknownUserId = UUID.randomUUID();
            given(userRepository.findById(unknownUserId)).willReturn(Optional.empty());

            AvatarEquipmentUpdateRequest request = new AvatarEquipmentUpdateRequest(List.of(UUID.randomUUID()));

            // when & then
            ApiException ex = assertThrows(ApiException.class,
                    () -> avatarItemService.updateEquipment(unknownUserId, request));
            assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("13. 실패 - 요청 목록에 존재하지 않는 아이템 ID 포함: ITEM_NOT_FOUND 예외 발생")
        void updateEquipment_InvalidItemId_ThrowsItemNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            UUID validItemId = UUID.randomUUID();
            UUID invalidItemId = UUID.randomUUID(); // DB에 없는 아이템

            User mockUser = mock(User.class);
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            // DB에서 1개만 반환 (요청은 2개 → 크기 불일치)
            Item mockItem = mock(Item.class);
            given(itemRepository.findAllById(anyList())).willReturn(List.of(mockItem));

            AvatarEquipmentUpdateRequest request =
                    new AvatarEquipmentUpdateRequest(List.of(validItemId, invalidItemId));

            // when & then
            ApiException ex = assertThrows(ApiException.class,
                    () -> avatarItemService.updateEquipment(userId, request));
            assertEquals(AvatarErrorCode.ITEM_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("14. 실패 - 레벨 부족 + 미획득 아이템 장착 시도 (해킹 방어): ITEM_NOT_OWNED 예외 발생")
        void updateEquipment_LevelLockedAndNotOwned_ThrowsItemNotOwned() {
            // given
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(1); // 유저 레벨 = 1 (부족)
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            Item mockItem = mock(Item.class);
            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getRequiredLevel()).willReturn(99); // 훨씬 높은 레벨 필요

            given(itemRepository.findAllById(List.of(itemId))).willReturn(List.of(mockItem));
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList()); // 인벤토리 비어 있음

            AvatarEquipmentUpdateRequest request = new AvatarEquipmentUpdateRequest(List.of(itemId));

            // when & then
            ApiException ex = assertThrows(ApiException.class,
                    () -> avatarItemService.updateEquipment(userId, request));
            assertEquals(AvatarErrorCode.ITEM_NOT_OWNED, ex.getErrorCode());
        }

        @Test
        @DisplayName("15. 성공 - 레벨 부족이지만 뽑기로 획득한 아이템은 정상 장착 가능")
        void updateEquipment_LevelLockedButOwnedByGacha_SuccessfullyEquipped() {
            // given
            UUID userId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            User mockUser = mock(User.class);
            given(mockUser.getLevel()).willReturn(1); // 유저 레벨 = 1 (부족)
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            Item mockItem = mock(Item.class);
            given(mockItem.getId()).willReturn(itemId);
            given(mockItem.getRequiredLevel()).willReturn(10); // 레벨 부족

            // 뽑기로 획득한 UserItem이 인벤토리에 존재
            UserItem gachaUserItem = mock(UserItem.class);
            given(gachaUserItem.getItem()).willReturn(mockItem);

            given(itemRepository.findAllById(List.of(itemId))).willReturn(List.of(mockItem));
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(List.of(gachaUserItem)); // 인벤토리에 있음
            given(userItemRepository.findEquippedItemsByUserId(userId)).willReturn(Collections.emptyList());
            given(userItemRepository.findByUserIdAndItemIdIn(userId, List.of(itemId))).willReturn(List.of(gachaUserItem));

            AvatarEquipmentUpdateRequest request = new AvatarEquipmentUpdateRequest(List.of(itemId));

            // when & then — 예외 없이 정상 완료
            assertDoesNotThrow(() -> avatarItemService.updateEquipment(userId, request));
            verify(gachaUserItem).equip();
        }

        @Test
        @DisplayName("16. 성공 - 빈 리스트로 장착 요청 시 전체 해제 (전체 unequip, save 미호출)")
        void updateEquipment_EmptyList_UnequipsAllAndSavesNothing() {
            // given
            UUID userId = UUID.randomUUID();

            User mockUser = mock(User.class);
            // targetItems가 빈 리스트이므로 권한 검증 for-loop가 실행되지 않아 getLevel()은 불필요 → 제거
            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

            // 빈 리스트이므로 findAllById는 빈 결과 반환
            given(itemRepository.findAllById(Collections.emptyList())).willReturn(Collections.emptyList());
            // findAllByUserIdWithItem은 서비스에서 항상 호출되므로 명시적으로 스터빙
            given(userItemRepository.findAllByUserIdWithItem(userId)).willReturn(Collections.emptyList());

            UserItem prevEquipped = mock(UserItem.class);
            given(userItemRepository.findEquippedItemsByUserId(userId)).willReturn(List.of(prevEquipped));

            AvatarEquipmentUpdateRequest request = new AvatarEquipmentUpdateRequest(Collections.emptyList());

            // when
            avatarItemService.updateEquipment(userId, request);

            // then
            verify(prevEquipped).unequip();
            verify(userItemRepository, never()).save(any(UserItem.class));
        }
    }
}
