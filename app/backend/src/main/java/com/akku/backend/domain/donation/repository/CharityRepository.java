package com.akku.backend.domain.donation.repository;

import com.akku.backend.domain.donation.entity.Charity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CharityRepository extends JpaRepository<Charity, UUID> {
}
