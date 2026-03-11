package com.akku.backend.domain.avatar.repository;

import com.akku.backend.domain.avatar.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserItemRepository extends JpaRepository<UserItem, UUID> {

    /**
     * 특정 유저가 보유한 모든 인벤토리 아이템 조회
     * N+1 문제 방지를 위해 JOIN FETCH로 Item 엔티티를 한 번에 끌고 옴
     */
    @Query("SELECT ui FROM UserItem ui JOIN FETCH ui.item WHERE ui.user.id = :userId")
    List<UserItem> findAllByUserIdWithItem(@Param("userId") UUID userId);

    /**
     * 장착 중인 아이템만 가져오기
     */
    @Query("SELECT ui FROM UserItem ui JOIN FETCH ui.item WHERE ui.user.id = :userId AND ui.isEquipped = true")
    List<UserItem> findEquippedItemsByUserId(@Param("userId") UUID userId);

    /**
     * 프론트엔드가 장착 요청한 아이템 ID 목록이 DB(user_items)에 이미 존재하는지 조회
     * (존재하면 Update, 존재하지 않으면 Insert 처리를 하기 위함)
     */
    List<UserItem> findByUserIdAndItemIdIn(UUID userId, List<UUID> itemIds);


}