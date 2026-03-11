package com.akku.backend.domain.avatar.service;

import com.akku.backend.domain.auth.entity.User;
import com.akku.backend.domain.auth.repository.UserRepository;
import com.akku.backend.domain.user.exception.UserErrorCode;
import com.akku.backend.domain.avatar.dto.AvatarItemListResponse;
import com.akku.backend.domain.avatar.dto.AvatarItemResponse;
import com.akku.backend.domain.avatar.entity.Item;
import com.akku.backend.domain.avatar.repository.ItemRepository;
import com.akku.backend.domain.avatar.repository.UserItemRepository;
import com.akku.backend.global.error.ApiException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
                    boolean isOwned = !isLevelLocked || ownedItemIds.contains(item.getId());

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
}