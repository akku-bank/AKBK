package com.akku.backend.domain.bank.controller;

import com.akku.backend.domain.bank.dto.OfflinePaymentTokenResponse;
import com.akku.backend.domain.bank.dto.PaymentApprovalRequest;
import com.akku.backend.domain.bank.dto.PaymentApprovalResponse;
import com.akku.backend.domain.bank.service.PaymentService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Bank Payment", description = "결제 관련 API")
@RestController
@RequestMapping("/api/bank/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/token")
    @Operation(summary = "오프라인 결제용 1회용 토큰 발급", description = "자녀 계정인 경우 3분간 유효한 12자리 결제 토큰 발급")
    public ApiResponse<OfflinePaymentTokenResponse> issueOfflinePaymentToken(@AuthenticationPrincipal UUID userId) {
        return ApiResponse.success("결제 토큰이 발급되었습니다.", paymentService.issueOfflinePaymentToken(userId));
    }

    @PostMapping
    @Operation(summary = "오프라인 결제 승인", description = "단말기에서 스캔한 토큰과 금액을 받아 결제 처리 및 5% 젤링 캐시백")
    public ApiResponse<PaymentApprovalResponse> approvePayment(
            @RequestHeader("x-api-key") String apiKey,
            @Valid @RequestBody PaymentApprovalRequest request) {
        return ApiResponse.success("결제가 성공적으로 승인되었습니다.", paymentService.approvePayment(request, apiKey));
    }
}
