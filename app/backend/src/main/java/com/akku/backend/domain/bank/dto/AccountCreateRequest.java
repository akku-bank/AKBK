package com.akku.backend.domain.bank.dto;

import java.util.UUID;

public record AccountCreateRequest(
    UUID childId,
    String accountType
) {}
