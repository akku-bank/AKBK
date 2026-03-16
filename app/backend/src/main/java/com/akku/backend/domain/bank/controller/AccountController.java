package com.akku.backend.domain.bank.controller;

import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.service.AccountService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Bank Account", description = "계좌 관리 API")
@RestController
@RequestMapping("/api/bank/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 생성", description = "부모가 자녀의 가상 계좌를 생성합니다.")
    @PostMapping
    public ApiResponse<AccountCreateResponse> createAccount(
            @RequestParam UUID parentId, 
            @RequestBody AccountCreateRequest request
    ) {
        AccountCreateResponse response = accountService.createAccount(parentId, request);
        return ApiResponse.success("계좌 생성 성공", response);
    }

    @Operation(summary = "내 계좌 목록 조회", description = "로그인한 사용자의 모든 연동 계좌 목록을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<AccountListResponse> getMyAccounts(
            @RequestParam UUID userId
    ) {
        AccountListResponse response = accountService.getMyAccounts(userId);
        return ApiResponse.success("계좌 목록 조회 성공", response);
    }

    @Operation(summary = "타행 계좌 연동", description = "타행 계좌를 서비스에 연동합니다.")
    @PostMapping("/link")
    public ApiResponse<Void> linkExternalAccount(
            @RequestParam UUID userId,
            @RequestBody AccountLinkRequest request
    ) {
        accountService.linkExternalAccount(userId, request);
        return ApiResponse.success("계좌 연동 성공");
    }
}
