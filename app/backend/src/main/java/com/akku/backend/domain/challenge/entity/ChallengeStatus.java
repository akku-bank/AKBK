package com.akku.backend.domain.challenge.entity;

public enum ChallengeStatus {
    PENDING,           // 승인 대기 중 (초기 상태)
    APPROVED,          // 부모 승인 완료
    REJECTED,          // 부모 거절
    IN_PROGRESS,       // 진행 중 (해당 주간 시작)
    SUCCESS,           // 목표 달성 성공
    FAIL,              // 목표 달성 실패
    REWARD_REQUESTED,  // 자녀가 보상(용돈) 요청함
    REWARDED           // 부모가 송금 완료함
}
