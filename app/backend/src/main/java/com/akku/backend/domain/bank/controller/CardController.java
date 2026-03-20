package com.akku.backend.domain.bank.controller;

import com.akku.backend.domain.bank.dto.*;
import com.akku.backend.domain.bank.service.CardService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cards", description = "카드 관리 API")
@RestController
@RequestMapping("/api/bank/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @Operation(summary = "카드 상품 목록 조회", description = "발급 가능한 카드 상품(종류) 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<CardProductResponse>> getCardProducts(
            @AuthenticationPrincipal UUID userId
    ) {
        List<CardProductResponse> response = cardService.getCardProducts(userId);
        return ApiResponse.success("카드 상품 목록 조회 완료", response);
    }

    @Operation(summary = "카드 발급", description = "특정 카드 상품을 선택하여 새로운 카드를 발급받습니다.")
    @PostMapping
    public ApiResponse<CardResponse> createCard(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CardCreateRequest request
    ) {
        CardResponse response = cardService.createCard(userId, request);
        return ApiResponse.success("카드 발급 완료", response);
    }

    @Operation(summary = "내 카드 목록 조회", description = "현재 사용자가 보유한 모든 카드의 목록을 조회합니다.")
    @GetMapping("/my")
    public ApiResponse<List<CardResponse>> getMyCards(
            @AuthenticationPrincipal UUID userId
    ) {
        List<CardResponse> response = cardService.getMyCards(userId);
        return ApiResponse.success("내 카드 목록 조회 완료", response);
    }

    @Operation(summary = "카드 결제", description = "보유한 카드로 결제를 진행합니다.")
    @PostMapping("/payment")
    public ApiResponse<Void> processPayment(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CardPaymentRequest request
    ) {
        cardService.processPayment(userId, request);
        return ApiResponse.success("카드 결제 완료");
    }

    @Operation(summary = "카드 거래 내역 조회", description = "특정 카드의 거래 내역을 조회합니다.")
    @PostMapping("/history")
    public ApiResponse<CardHistoryResponse> getCardHistory(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CardHistoryRequest request
    ) {
        CardHistoryResponse response = cardService.getCardHistory(userId, request);
        return ApiResponse.success("카드 거래 내역 조회 완료", response);
    }
}
