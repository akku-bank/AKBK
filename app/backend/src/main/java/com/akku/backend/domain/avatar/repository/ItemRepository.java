package com.akku.backend.domain.avatar.repository;

import com.akku.backend.domain.avatar.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    /**
     * 특정 카테고리의 아이템 목록만 조회 (예: category 파라미터가 "CLOTHES"일 때)
     */
    List<Item> findByCategory(String category);
}