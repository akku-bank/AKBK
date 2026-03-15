package com.akku.backend.domain.bank.repository;

import com.akku.backend.domain.bank.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    List<Card> findAllByUserId(UUID userId);
}
