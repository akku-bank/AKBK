package com.akku.backend.domain.social.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "friends")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friend {

    @EmbeddedId
    private FriendId id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Friend(FriendId id) {
        this.id = id;
    }
}
