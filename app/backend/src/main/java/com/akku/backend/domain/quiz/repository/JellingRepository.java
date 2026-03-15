package com.akku.backend.domain.quiz.repository;

import com.akku.backend.domain.quiz.entity.Jelling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * jellings 테이블 레포지토리
 */
public interface JellingRepository extends JpaRepository<Jelling, UUID> {
    // findById(UUID userId) / save() — Spring Data 기본 제공
}
