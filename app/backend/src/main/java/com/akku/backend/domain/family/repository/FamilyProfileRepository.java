package com.akku.backend.domain.family.repository;

import com.akku.backend.domain.family.entity.FamilyProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyProfileRepository extends JpaRepository<FamilyProfileEntity, UUID> {

    // 특정 가족 그룹에서 이름과 생일이 일치하는 프로필 조회 (연동 여부 무관)
    Optional<FamilyProfileEntity> findByFamilyIdAndNameAndBirthDate(
            UUID familyId, String name, java.time.LocalDate birthDate);

    // 특정 가족 그룹에 속한 연동되지 않은(빈 의자) 프로필 중 이름과 생일이 일치하는 것 찾기
    Optional<FamilyProfileEntity> findByFamilyIdAndNameAndBirthDateAndLinkedUserIdIsNull(
            UUID familyId, String name, java.time.LocalDate birthDate);

    // 특정 가족 그룹의 모든 구성원 프로필 조회
    List<FamilyProfileEntity> findAllByFamilyId(UUID familyId);
}