package com.akku.backend.domain.bank.controller;

import com.akku.backend.domain.bank.dto.TransactionHistoryResponse;
import com.akku.backend.domain.bank.service.TransactionService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Bank Transaction", description = "거래 내역 조회 API")
@RestController
@RequestMapping("/api/bank/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "내역 달력 조회", description = "입력받은 연도/월의 모든 계좌 거래 내역을 조회합니다.")
    @GetMapping
    public ApiResponse<TransactionHistoryResponse> getTransactionHistory(
            @AuthenticationPrincipal UUID userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        TransactionHistoryResponse response = transactionService.getTransactionHistory(userId, year, month);
        return ApiResponse.success("거래 내역 조회 성공", response);
    }

    @Operation(summary = "자녀 세부 소비 내역", description = "부모가 자녀의 거래 내역을 조회합니다. 숨김 처리된 내역은 마스킹됩니다.")
    @GetMapping("/{childId}")
    public ApiResponse<TransactionHistoryResponse> getChildTransactionHistory(
            @PathVariable UUID childId,
            @AuthenticationPrincipal UUID parentId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        TransactionHistoryResponse response = transactionService.getChildTransactionHistory(parentId, childId, year, month);
        return ApiResponse.success("자녀 거래 내역 조회 성공", response);
    }
}
