from ai.schemas.policy import PolicyDecision, PolicyReason, PolicyResult
from ai.schemas.state import ChatContextState


class PolicyService:
    CREDIT_LIMIT_MESSAGE = "크레딧을 모두 사용했습니다. 정답을 제출해주세요."

    def run_policy_gate(self, state: ChatContextState) -> PolicyResult:
        # 크레딧 한도 체크
        credit_result = self._check_credit_limit(state)
        if credit_result is not None:
            return credit_result

        # 정책 통과
        return PolicyResult(
            decision=PolicyDecision.ALLOW,
            reason=PolicyReason.NONE,
            message=None,
        )

    def _check_credit_limit(self, state: ChatContextState) -> PolicyResult | None:
        # 완전 소진 상태인지 / 아예 요청 불가 상태인지만 판단
        # 나중에 route 결정되면 
        # 선택된 route를 실행할 만큼 크레딧이 충분한지 다시 확인 필요
        if state.credits_balance <= 0:
            return PolicyResult(
                decision=PolicyDecision.DENY,
                reason=PolicyReason.CREDIT_LIMIT,
                message=self.CREDIT_LIMIT_MESSAGE,
            )
        return None
