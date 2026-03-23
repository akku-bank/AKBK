package com.akku.backend.domain.bank.controller;

import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.service.AccountService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
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
            @AuthenticationPrincipal UUID parentId, 
            @Valid @RequestBody AccountCreateRequest request
    ) {
        AccountCreateResponse response = accountService.createAccount(parentId, request);
        return ApiResponse.success("계좌 생성 성공", response);
    }

    @Operation(summary = "내 계좌 목록 조회", description = "로그인한 사용자의 모든 연동 계좌 목록을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<AccountListResponse> getMyAccounts(
            @AuthenticationPrincipal UUID userId
    ) {
        AccountListResponse response = accountService.getMyAccounts(userId);
        return ApiResponse.success("계좌 목록 조회 성공", response);
    }

    @Operation(summary = "타행 계좌 연동", description = "타행 계좌를 서비스에 연동합니다.")
    @PostMapping("/link")
    public ApiResponse<Void> linkExternalAccount(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AccountLinkRequest request
    ) {
        accountService.linkExternalAccount(userId, request);
        return ApiResponse.success("계좌 연동 성공");
    }

    @Operation(summary = "주계좌 지정", description = "특정 계좌를 주계좌로 설정합니다. 기존 주계좌는 자동 해제됩니다.")
    @PatchMapping("/{accountId}/primary")
    public ApiResponse<Void> setPrimaryAccount(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID accountId
    ) {
        accountService.setPrimaryAccount(userId, accountId);
        return ApiResponse.success("주계좌가 설정되었습니다.");
    }

    @Operation(summary = "계좌 이체", description = "입력받은 계좌/금액으로 송금 처리 후 잔액 갱신 및 내역을 생성합니다.")
    @PostMapping("/transfers")
    public ApiResponse<TransferResponse> transfer(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody TransferRequest request
    ) {
        TransferResponse response = accountService.transfer(userId, request);
        return ApiResponse.success("계좌 이체 성공", response);
    }
}
