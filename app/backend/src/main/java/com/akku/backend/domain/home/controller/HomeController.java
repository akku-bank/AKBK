package com.akku.backend.domain.home.controller;

import com.akku.backend.domain.home.dto.ChildHomeResponse;
import com.akku.backend.domain.home.dto.ParentHomeResponse;
import com.akku.backend.domain.home.service.HomeService;
import com.akku.backend.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 자녀 홈 대시보드 조회
     * @param userId 로그인한 자녀의 ID
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ChildHomeResponse>> getChildHome(
            @AuthenticationPrincipal UUID userId
    ) {
        ChildHomeResponse data = homeService.getChildHome(userId);

        return ResponseEntity.ok(
                ApiResponse.success("홈 데이터를 성공적으로 불러왔습니다.", data)
        );
    }


    /**
     * 부모 대시보드 홈 조회
     * @param userId 로그인한 부모의 ID
     */
    @GetMapping("/parent")
    public ResponseEntity<ApiResponse<ParentHomeResponse>> getParentHome(
            @AuthenticationPrincipal UUID userId
    ) {
        ParentHomeResponse data = homeService.getParentHome(userId);

        return ResponseEntity.ok(
                ApiResponse.success("부모 대시보드 데이터를 성공적으로 불러왔습니다.", data)
        );
    }


}