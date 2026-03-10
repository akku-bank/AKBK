package com.akku.backend.domain.family.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 가족 그룹(Family) 엔티티
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "families")
public class FamilyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // UUID 자동 생성 전략
    private UUID id;

    @Column(unique = true, length = 100)
    private String qrCode;

    // QR 코드 만료시간은 QR 생성 시 비즈니스 로직에서 처리
    private LocalDateTime qrExpiresAt;

    @CreatedDate
    @Column(updatable = false)  // 생성일은 한 번 저장되면 수정 불가
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * 가족 그룹 최초 생성 시 사용하는 빌더
     * @param qrCode 발급된 QR 코드 문자열
     * @param qrExpiresAt QR 코드 만료 일시
     */
    @Builder
    public FamilyEntity(String qrCode, LocalDateTime qrExpiresAt){
        this.qrCode = qrCode;
        this.qrExpiresAt = qrExpiresAt;
    }

    /**
     * 가족 초대 QR 코드 재발급 비즈니스 로직
     * Setter 대신 상태 변경하는 메서드 사용
     */
    public void updateQrCode(String newQrCode, LocalDateTime newQrExpiresAt){
        this.qrCode = newQrCode;
        this.qrExpiresAt = newQrExpiresAt;
    }

}
