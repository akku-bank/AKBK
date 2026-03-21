package com.akku.backend.domain.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "friend_invites")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendInvite {

    @Id
    @Column(name = "invite_code", length = 100)
    private String inviteCode;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public FriendInvite(String inviteCode, UUID userId) {
        this.inviteCode = inviteCode;
        this.userId = userId;
    }
}
