package com.akku.backend.domain.family.repository;


import com.akku.backend.domain.family.entity.FamilyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FamilyRepository extends JpaRepository<FamilyEntity, UUID> {

    // 1. 가족 QR로 가족 그룹 찾기
    Optional<FamilyEntity> findByQrCode(String qrCode);


}
