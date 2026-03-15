package com.akku.backend.domain.bank.dto;

import java.util.List;

public record AccountListResponse(
    List<AccountInfo> accounts
) {}
