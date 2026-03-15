package com.akku.backend.domain.quiz.repository;

import com.akku.backend.domain.quiz.entity.JellingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * jelling_transactions 테이블 레포지토리
 */
public interface JellingTransactionRepository extends JpaRepository<JellingTransaction, UUID> {
    // save() — Spring Data 기본 제공
}
