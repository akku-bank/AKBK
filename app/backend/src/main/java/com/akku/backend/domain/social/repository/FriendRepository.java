package com.akku.backend.domain.social.repository;

import com.akku.backend.domain.social.entity.Friend;
import com.akku.backend.domain.social.entity.FriendId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FriendRepository extends JpaRepository<Friend, FriendId> {
    List<Friend> findAllByIdUserId(UUID userId);
}
