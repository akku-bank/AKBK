package com.akku.backend.domain.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AccountCreateRequest(
    @NotNull(message = "자녀 ID는 필수입니다.")
    UUID childId,

    @NotBlank(message = "계좌 타입은 필수입니다.")
    String accountType
) {}
