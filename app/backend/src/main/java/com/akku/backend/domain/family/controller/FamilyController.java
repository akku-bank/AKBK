package com.akku.backend.domain.family.controller;

import com.akku.backend.domain.family.dto.*;
import com.akku.backend.domain.family.service.FamilyService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 가족 도메인 프론트엔드 통신 API
 */
@Tag(name = "Family API", description = "가족 그룹 생성 및 구성원 관리")
@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    /**
     * 1. 가족 그룹 생성
     */
    @Operation(summary = "가족 그룹 생성", description = "빈 가족 그룹을 생성하고 부모의 family_id를 업데이트합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "가족 그룹 생성 성공")
    @PostMapping
    public ResponseEntity<ApiResponse<FamilyCreateResponse>> createFamilyGroup(
            @AuthenticationPrincipal UUID parentId) {

        FamilyCreateResponse responseDto = familyService.createFamilyGroup(parentId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("가족 그룹이 성공적으로 생성되었습니다.", responseDto));
    }

    /**
     * 1-1. 가족 구성원 사전 등록 (미연동 프로필 생성)
     */
    @Operation(summary = "가족 구성원 사전 등록", description = "부모가 자녀의 이름과 생일을 미리 등록하여 연동 대기 프로필을 생성합니다.")
    @PostMapping("/members")
    public ResponseEntity<ApiResponse<Void>> preRegisterFamilyMember(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody FamilyMemberPreRegisterRequest request) {

        familyService.preRegisterFamilyMember(userId, request);

        return ResponseEntity.ok(ApiResponse.success("가족 구성원이 사전 등록되었습니다.", null));
    }

    /**
     * 2. 가족 QR 발급 및 조회
     * 부모가 자녀 초대용 QR 코드를 요청
     */
    @Operation(summary = "가족 QR 발급", description = "유효한 QR 코드를 조회하거나 새로 생성하여 반환합니다.")
    @GetMapping("/qr")
    public ResponseEntity<ApiResponse<FamilyQrResponse>> getFamilyQr(
            @AuthenticationPrincipal UUID userId) {

        FamilyQrResponse response = familyService.getOrGenerateFamilyQr(userId);

        return ResponseEntity.ok(
                ApiResponse.success("QR 코드가 성공적으로 조회되었습니다.", response)
        );
    }

    /**
     * 3. 가족 그룹 합류 (QR 스캔 및 자동 매칭)
     */
    @Operation(summary = "가족 그룹 합류", description = "QR 스캔과 이름/생일 대조를 통해 가족 그룹에 자동 합류합니다.")
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> joinFamilyGroup(
            @AuthenticationPrincipal UUID childId,
            @Valid @RequestBody FamilyJoinRequest request) {

        familyService.joinFamilyGroup(childId, request.scannedQrCode());

        return ResponseEntity.ok(ApiResponse.success("가족 그룹에 성공적으로 합류했습니다.", null));
    }

    /**
     * 4. 가족 QR 재발급
     */
    @Operation(summary = "가족 QR 재발급", description = "기존 QR을 무효화하고 새로운 가족 초대 QR을 강제 발급합니다.")
    @PostMapping("/qr/reissue")
    public ResponseEntity<ApiResponse<FamilyQrResponse>> reissueFamilyQr(
            @AuthenticationPrincipal UUID userId) {

        FamilyQrResponse response = familyService.reissueFamilyQr(userId);

        return ResponseEntity.ok(
                ApiResponse.success("QR 코드가 성공적으로 재발급되었습니다.", response)
        );
    }

    /**
     * 5. 가족 구성원 조회
     */
    @Operation(summary = "가족 구성원 조회", description = "가족 그룹에 속한 구성원 목록과 계좌 정보를 조회합니다.")
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<FamilyMemberListResponse>> getFamilyMembers(
            @AuthenticationPrincipal UUID userId) {

        FamilyMemberListResponse response = familyService.getFamilyMembers(userId);

        return ResponseEntity.ok(
                ApiResponse.success("가족 구성원 목록을 조회했습니다.", response)
        );
    }

    /**
     * 6. 가족 QR 수동 만료
     */
    @Operation(summary = "가족 QR 만료", description = "발급된 가족 초대 QR 코드를 즉시 무효화(만료) 처리합니다.")
    @DeleteMapping("/qr")
    public ResponseEntity<ApiResponse<Void>> expireFamilyQr(
            @AuthenticationPrincipal UUID userId) {

        familyService.expireFamilyQr(userId);

        return ResponseEntity.ok(
                ApiResponse.success("QR 코드가 성공적으로 만료 처리되었습니다.", null)
        );
    }

    /**
     * 7. 가족 구성원 정보 수정
     */
    @Operation(summary = "가족 구성원 정보 수정", description = "미연동 상태인 가족 구성원의 이름과 생년월일을 수정합니다. (연동 완료 시 수정 불가)")
    @PatchMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> updateFamilyMember(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("memberId") UUID memberId,
            @RequestBody FamilyMemberUpdateRequest request) {

        familyService.updateFamilyMember(userId, memberId, request);

        return ResponseEntity.ok(
                ApiResponse.success("구성원 정보가 성공적으로 수정되었습니다.", null)
        );
    }

    /**
     * 8. 가족 구성원 연결 해제
     */
    @Operation(summary = "가족 구성원 연결 해제", description = "연동된 자녀의 계정 연결을 해제하거나, 미연동 프로필을 완전히 삭제합니다.")
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeFamilyMember(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("memberId") UUID memberId) {

        familyService.removeFamilyMember(userId, memberId);

        return ResponseEntity.ok(
                ApiResponse.success("가족 구성원 연결이 성공적으로 해제되었습니다.", null)
        );
    }
}
