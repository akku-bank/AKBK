package com.akku.backend.domain.bank.repository;

import com.akku.backend.domain.bank.entity.CardProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardProductRepository extends JpaRepository<CardProduct, UUID> {
    Optional<CardProduct> findByCardUniqueNo(String cardUniqueNo);
}
