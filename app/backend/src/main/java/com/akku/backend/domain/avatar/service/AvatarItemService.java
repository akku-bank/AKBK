package com.akku.backend.domain.avatar.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.avatar.dto.AvatarEquipmentUpdateRequest;
import com.akku.backend.domain.avatar.dto.AvatarItemListResponse;
import com.akku.backend.domain.avatar.dto.AvatarItemResponse;
import com.akku.backend.domain.avatar.entity.Item;
import com.akku.backend.domain.avatar.entity.UserItem;
import com.akku.backend.domain.avatar.exception.AvatarErrorCode;
import com.akku.backend.domain.avatar.repository.ItemRepository;
import com.akku.backend.domain.avatar.repository.UserItemRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.global.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvatarItemService {

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;

    /**
     * 아이템 도감(전체 목록) 조회
     */
    public AvatarItemListResponse getAvatarItems(UUID userId, String category) {

        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        // 2. 도감 아이템 목록 조회 (카테고리 필터링)
        List<Item> items = (category != null && !category.isBlank())
                ? itemRepository.findByCategory(category)
                : itemRepository.findAll();

        // 3. 내 인벤토리(기부 뽑기 획득) 아이템 ID 목록 추출 (Set으로 성능 최적화)
        Set<UUID> ownedItemIds = userItemRepository.findAllByUserIdWithItem(userId).stream()
                .map(userItem -> userItem.getItem().getId())
                .collect(Collectors.toSet());

        // 4. 비즈니스 로직 적용 및 DTO 변환
        List<AvatarItemResponse> itemResponses = items.stream()
                .map(item -> {
                    boolean isLevelLocked = item.getRequiredLevel() > user.getLevel();
                    
                    // 기부 아이템인지 일반 의상인지 구분
                    boolean isCharityItem = !"HAT".equals(item.getCategory()) && 
                                            !"TOP".equals(item.getCategory()) && 
                                            !"BOTTOM".equals(item.getCategory());

                    boolean isOwned;
                    if (isCharityItem) {
                        // 기부 아이템은 레벨과 상관없이 실제로 획득(ownedItemIds에 존재)해야만 소유한 것으로 간주
                        isOwned = ownedItemIds.contains(item.getId());
                    } else {
                        // 일반 의상은 레벨만 충족하면 자동으로 소유 가능
                        isOwned = !isLevelLocked || ownedItemIds.contains(item.getId());
                    }

                    return new AvatarItemResponse(
                            item.getId(),
                            item.getCategory(),
                            item.getName(),
                            item.getResourceUrl(),
                            item.getRequiredLevel(),
                            isOwned,
                            isLevelLocked
                    );
                })
                .collect(Collectors.toList());

        return new AvatarItemListResponse(itemResponses);
    }

    /**
     * 아이템 장착/변경 (일괄 업데이트)
     */
    @Transactional // 데이터 변경이 일어나므로 읽기 전용 해제
    public void updateEquipment(UUID userId, AvatarEquipmentUpdateRequest request) {

        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));

        List<UUID> requestedItemIds = request.equippedItemIds();

        // 2. 요청받은 아이템들이 실제로 존재하는지 조회
        List<Item> targetItems = itemRepository.findAllById(requestedItemIds);
        if (targetItems.size() != requestedItemIds.size()) {
            throw new ApiException(AvatarErrorCode.ITEM_NOT_FOUND); // 유효하지 않은 아이템 ID가 섞여 있음
        }

        // 3. 장착 권한 검증
        // 유저가 보유한 전체 아이템을 가져와서 검증에 사용
        Set<UUID> ownedItemIds = userItemRepository.findAllByUserIdWithItem(userId).stream()
                .map(ui -> ui.getItem().getId())
                .collect(Collectors.toSet());

        for (Item item : targetItems) {
            boolean isLevelLocked = item.getRequiredLevel() > user.getLevel();
            boolean isCharityItem = !"HAT".equals(item.getCategory()) && 
                                    !"TOP".equals(item.getCategory()) && 
                                    !"BOTTOM".equals(item.getCategory());

            boolean isOwned = isCharityItem 
                    ? ownedItemIds.contains(item.getId()) 
                    : (!isLevelLocked || ownedItemIds.contains(item.getId()));

            if (!isOwned) {
                // 내 레벨로도 못 입고, 뽑기로도 못 뽑은 템을 입으려고 시도함 (해킹 방어)
                throw new ApiException(AvatarErrorCode.ITEM_NOT_OWNED);
            }
        }

        // 4. 기존에 장착 중이던 아이템 모두 해제 (isEquipped = false)
        List<UserItem> currentlyEquipped = userItemRepository.findEquippedItemsByUserId(userId);
        for (UserItem ui : currentlyEquipped) {
            ui.unequip();
        }

        // 5. 새로운 아이템 장착 (Upsert 로직 적용)
        List<UserItem> existingUserItems = userItemRepository.findByUserIdAndItemIdIn(userId, requestedItemIds);

        // 빠른 검색을 위해 List를 Map으로 변환 (Key: Item ID, Value: UserItem 엔티티)
        Map<UUID, UserItem> existingUserItemMap = existingUserItems.stream()
                .collect(Collectors.toMap(ui -> ui.getItem().getId(), ui -> ui));

        for (Item item : targetItems) {
            if (existingUserItemMap.containsKey(item.getId())) {
                // 5-1. 이미 user_items에 존재하면 상태만 업데이트 (Update)
                existingUserItemMap.get(item.getId()).equip();
            } else {
                // 5-2. 레벨업 후 한 번도 안 입어본 새 옷이라면 새로 생성 (Insert)
                UserItem newUserItem = UserItem.builder()
                        .user(user)
                        .item(item)
                        .isEquipped(true)
                        .build();
                userItemRepository.save(newUserItem);
            }
        }
    }
}