package com.akku.backend.domain.family.controller;

import com.akku.backend.domain.family.dto.FamilyCreateResponse;
import com.akku.backend.domain.family.dto.FamilyJoinRequest;
import com.akku.backend.domain.family.dto.FamilyMemberPreRegisterRequest;
import com.akku.backend.domain.family.service.FamilyService;
import com.akku.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @RequestAttribute("userId") UUID parentId) {

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
            @RequestAttribute("familyId") UUID familyId,
            @Valid @RequestBody FamilyMemberPreRegisterRequest request) {

        familyService.preRegisterFamilyMember(familyId, request);

        return ResponseEntity.ok(ApiResponse.success("가족 구성원이 사전 등록되었습니다.", null));
    }

    /**
     * 3. 가족 그룹 합류 (QR 스캔 및 자동 매칭)
     */
    @Operation(summary = "가족 그룹 합류", description = "QR 스캔과 이름/생일 대조를 통해 가족 그룹에 자동 합류합니다.")
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> joinFamilyGroup(
            @RequestAttribute("userId") UUID childId,
            @RequestAttribute("name") String name,
            @RequestAttribute("birthDate") String birthDate,
            @Valid @RequestBody FamilyJoinRequest request) {

        familyService.joinFamilyGroup(childId, request.scannedQrCode(), name, java.time.LocalDate.parse(birthDate));

        return ResponseEntity.ok(ApiResponse.success("가족 그룹에 성공적으로 합류했습니다.", null));
    }
}