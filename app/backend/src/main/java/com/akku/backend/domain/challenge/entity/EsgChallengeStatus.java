package com.akku.backend.domain.challenge.entity;

public enum EsgChallengeStatus {
    IN_PROGRESS,  // 진행 중 (Lazy Insert 시 초기값)
    SUCCESS,      // 녹색 가맹점 결제로 달성
    REWARDED      // 보상 수령 완료
}
