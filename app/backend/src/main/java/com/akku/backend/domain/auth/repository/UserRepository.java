package com.akku.backend.domain.auth.repository;

import com.akku.backend.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // 보상 요청 FCM 알림용 — 같은 가족 내 부모 전체 조회 (엄마+아빠 모두 알림 발송)
    List<User> findAllByFamilyIdAndRole(UUID familyId, String role);

}
