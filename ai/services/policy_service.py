from ai.schemas.policy import PolicyDecision, PolicyReason, PolicyResult
from ai.schemas.state import ChatContextState


class PolicyService:
    CREDIT_LIMIT_MESSAGE = "크레딧을 모두 사용했습니다. 정답을 제출해주세요."
    OUT_OF_SCOPE_MESSAGE = "금융 관련 질문만 가능합니다."
    CHEATING_MESSAGE = "정답은 직접 제공할 수 없습니다."

    FINANCE_KEYWORDS = [
        "금융",
        "이자",
        "금리",
        "복리",
        "원금",
        "예금",
        "적금",
        "대출",
        "주식",
        "채권",
        "펀드",
        "투자",
        "보험",
        "세금",
        "소득",
        "지출",
        "저축",
        "자산",
        "부채",
        "신용",
        "카드",
        "용돈",
        "소비",
        "예산",
        "경제",
        "물가",
        "환율",
    ]

    QUIZ_CONTEXT_KEYWORDS = [
        "힌트",
        "설명",
        "문제",
        "퀴즈",
        "이 문제",
        "이 퀴즈",
        "보기",
        "선지",
        "이해",
        "모르겠어",
        "모르겠어요",
    ]

    CHEATING_KEYWORDS = [
        "정답 알려줘",
        "정답 뭐야",
        "답 알려줘",
        "답 뭐야",
        "정답만",
        "답만",
        "몇 번이야",
        "몇번이야",
        "정답이 뭐야",
        "정답 알려",
        "답 알려",
        "맞는 번호 알려줘",
        "정답 번호 알려줘",
        "보기 중 뭐가 맞아",
    ]

    def run_policy_gate(self, state: ChatContextState) -> PolicyResult:
        # 1. 크레딧 한도 체크
        credit_result = self._check_credit_limit(state)
        if credit_result is not None:
            return credit_result
        # 2. 금융 범위 체크
        scope_result = self._check_finance_scope(state)
        if scope_result is not None:
            return scope_result
        # 3. 부정행위 체크
        cheating_result = self._check_cheating(state)
        if cheating_result is not None:
            return cheating_result
        # 4. 모든 정책 통과
        return PolicyResult(
            decision=PolicyDecision.ALLOW,
            reason=PolicyReason.NONE,
            message=None,
        )

    def _check_credit_limit(self, state: ChatContextState) -> PolicyResult | None:
        if state.credits_balance <= 0:
            return PolicyResult(
                decision=PolicyDecision.DENY,
                reason=PolicyReason.CREDIT_LIMIT,
                message=self.CREDIT_LIMIT_MESSAGE,
            )
        return None

    def _check_finance_scope(self, state: ChatContextState) -> PolicyResult | None:
        normalized_message = self._normalize_text(state.message)

        # 금융 키워드 있으면 허용
        has_finance_keyword = any(
            keyword in normalized_message for keyword in self.FINANCE_KEYWORDS
        )
        # 퀴즈 맥락 키워드 있으면 허용
        has_quiz_context_keyword = any(
            keyword in normalized_message for keyword in self.QUIZ_CONTEXT_KEYWORDS
        )

        if not has_finance_keyword and not has_quiz_context_keyword:
            return PolicyResult(
                decision=PolicyDecision.DENY,
                reason=PolicyReason.OUT_OF_SCOPE,
                message=self.OUT_OF_SCOPE_MESSAGE,
            )
        return None

    def _check_cheating(self, state: ChatContextState) -> PolicyResult | None:
        normalized_message = self._normalize_text(state.message)
        # 정답 요구하는 부정행위 키워드 체크
        is_cheating = any(
            keyword in normalized_message for keyword in self.CHEATING_KEYWORDS
        )

        if is_cheating:
            return PolicyResult(
                decision=PolicyDecision.DENY,
                reason=PolicyReason.CHEATING,
                message=self.CHEATING_MESSAGE,
            )
        return None

    @staticmethod
    def _normalize_text(text: str) -> str:
        # 텍스트를 소문자로 변환하고 양쪽 공백 제거
        # 문자열 비교 전 간단히 정리하는 용도
        return text.strip().lower()