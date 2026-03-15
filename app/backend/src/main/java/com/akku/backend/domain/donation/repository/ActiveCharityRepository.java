package com.akku.backend.domain.donation.repository;

import com.akku.backend.domain.donation.entity.ActiveCharity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActiveCharityRepository extends JpaRepository<ActiveCharity, UUID> {
    Optional<ActiveCharity> findByUserIdAndStatus(UUID userId, String status);
    
    boolean existsByUserIdAndStatus(UUID userId, String status);
}
