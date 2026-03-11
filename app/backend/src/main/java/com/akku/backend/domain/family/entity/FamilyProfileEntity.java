package com.akku.backend.domain.family.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 가족 구성원 프로필(빈 의자) 엔티티
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "family_profiles")
public class FamilyProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    // 실제 가입한 유저의 ID (연동 전에는 null)
    private UUID linkedUserId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public FamilyProfileEntity(UUID familyId, String role, String name, LocalDate birthDate) {
        this.familyId = familyId;
        this.role = role;
        this.name = name;
        this.birthDate = birthDate;
    }

    /**
     * 자녀 가입 시 실제 유저 ID와 연동하는 비즈니스 로직
     */
    public void linkUser(UUID userId) {
        this.linkedUserId = userId;
    }

    /**
     * 구성원 정보(이름, 생년월일) 부분 수정 로직
     * null이거나 빈 값이면 기존 데이터를 유지합니다.
     */
    public void updateProfileInfo(String name, LocalDate birthDate) {
        // 이름이 null이 아니고 빈 칸이 아닐 때만 수정
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }

        // 생년월일이 null이 아닐 때만 수정
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
    }

    /**
     * 유저 연동 해제 (Soft Disconnect)
     */
    public void unlinkUser() {
        this.linkedUserId = null;
    }

}