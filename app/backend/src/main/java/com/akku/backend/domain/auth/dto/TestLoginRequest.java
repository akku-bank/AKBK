package com.akku.backend.domain.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TestLoginRequest(
        @NotNull(message = "userId는 필수입니다.")
        UUID userId
) {}
