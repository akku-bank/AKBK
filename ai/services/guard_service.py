# ai/services/guard_service.py
# GuardService는 생성된 답변이 사용자에게 그대로 전달 가능한지 검사하는 서비스 계층입니다.
import re
from ai.schemas.guard import GuardReason, GuardResult


class GuardService:
    # 현재는 핵심 3개 규칙만 검사합니다.
    # 추후 소거법 유도, 연령 부적절 표현 같은 규칙으로 확장할 수 있습니다.
    ANSWER_LEAK_MESSAGE = "정답을 직접 드러내지 않도록 다시 작성해야 합니다."
    CHOICE_NUMBER_MESSAGE = "보기 번호를 직접 언급하지 않도록 다시 작성해야 합니다."
    TOO_DIRECT_MESSAGE = "힌트가 너무 직설적이므로 더 완곡하게 다시 작성해야 합니다."

    ANSWER_LEAK_PATTERNS = [
        "정답은",
        "답은",
        "맞는 답은",
        "정답이야",
        "정답입니다",
    ]

    TOO_DIRECT_PATTERNS = [
        "고르면 됩니다",
        "선택하면 됩니다",
        "이걸 고르면 돼",
        "이게 답이야",
        "그대로 쓰면 돼",
    ]

    CHOICE_NUMBER_PATTERN = re.compile(r"(?<!\d)([1-4])\s*번")

    def check_output(self, answer: str) -> GuardResult:
        # 생성된 답변을 검사해 그대로 사용자에게 전달 가능한지 판단합니다.
        normalized_answer = self._normalize_text(answer)
        if not normalized_answer:
            raise ValueError("guard 검사 대상 answer는 비어 있을 수 없습니다.")

        if self._contains_answer_leak(normalized_answer):
            return GuardResult(
                allowed=False,
                reason=GuardReason.ANSWER_LEAK,
                message=self.ANSWER_LEAK_MESSAGE,
            )

        if self._contains_choice_number_mention(normalized_answer):
            return GuardResult(
                allowed=False,
                reason=GuardReason.CHOICE_NUMBER_MENTION,
                message=self.CHOICE_NUMBER_MESSAGE,
            )

        if self._is_too_direct_hint(normalized_answer):
            return GuardResult(
                allowed=False,
                reason=GuardReason.TOO_DIRECT,
                message=self.TOO_DIRECT_MESSAGE,
            )

        return GuardResult(
            allowed=True,
            reason=GuardReason.NONE,
            message=None,
        )

    def _contains_answer_leak(self, normalized_answer: str) -> bool:
        # 정답을 직접 노출하는 표현이 포함되어 있는지 검사합니다.
        return any(pattern in normalized_answer for pattern in self.ANSWER_LEAK_PATTERNS)

    def _contains_choice_number_mention(self, normalized_answer: str) -> bool:
        # 1번, 2번처럼 보기 번호를 직접 말하는 표현이 있는지 검사합니다.
        return self.CHOICE_NUMBER_PATTERN.search(normalized_answer) is not None

    def _is_too_direct_hint(self, normalized_answer: str) -> bool:
        # 힌트가 지나치게 직접적이라 사실상 정답 유도로 보이는지 검사합니다.
        return any(pattern in normalized_answer for pattern in self.TOO_DIRECT_PATTERNS)

    @staticmethod
    def _normalize_text(text: str) -> str:
        # 문자열 비교를 쉽게 하기 위해 공백과 대소문자를 정리합니다.
        return text.strip().lower()
