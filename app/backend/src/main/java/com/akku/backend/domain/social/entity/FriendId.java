package com.akku.backend.domain.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class FriendId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "friend_id")
    private UUID friendId;
}
