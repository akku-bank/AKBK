package com.akku.backend.domain.bank.repository;

import com.akku.backend.domain.bank.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
}
