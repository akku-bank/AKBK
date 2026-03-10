package com.akku.backend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String email;

    @Column(length = 20)
    private String provider;        // KAKAO, GOOGLE 등

    @Column(name = "provider_id", length = 100)
    private String providerId;      // 카카오 고유 ID

    @Column(name = "fin_user_key")
    private String userKey;         // 금융망 userKey

    @Column(name = "family_id")
    private UUID familyId;

    @Column(nullable = false, length = 20)
    private String role;            // PARENT, CHILD

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "pin_password")
    private String pinPassword;

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void updateUserKey(String userKey) {
        this.userKey = userKey;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateProfile(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public void deactivate() {
        this.isActive = false;
    }
}


