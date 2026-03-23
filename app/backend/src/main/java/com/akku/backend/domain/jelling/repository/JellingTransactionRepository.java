package com.akku.backend.domain.jelling.repository;

import com.akku.backend.domain.jelling.entity.JellingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JellingTransactionRepository extends JpaRepository<JellingTransaction, UUID> {
}
