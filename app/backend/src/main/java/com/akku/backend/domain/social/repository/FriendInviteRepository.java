package com.akku.backend.domain.social.repository;

import com.akku.backend.domain.social.entity.FriendInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FriendInviteRepository extends JpaRepository<FriendInvite, String> {
    Optional<FriendInvite> findByUserId(UUID userId);
}
