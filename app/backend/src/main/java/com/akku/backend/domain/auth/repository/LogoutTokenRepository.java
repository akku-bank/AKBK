package com.akku.backend.domain.auth.repository;

import com.akku.backend.domain.auth.entity.LogoutToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogoutTokenRepository extends JpaRepository<LogoutToken, String> {

    boolean existsByToken(String token);
}
