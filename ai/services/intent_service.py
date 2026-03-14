from ai.clients.llm_client import LLMClient
from ai.schemas.intent import ClassificationResult, IntentType


class IntentService:
    def __init__(self) -> None:
        self.llm_client = LLMClient()

    def classify_message(self, message: str) -> ClassificationResult:
        # 질문 분류용 LLM 호출 결과를 내부 ClassificationResult 모델로 변환합니다.
        result = self.llm_client.classify_message(message)

        return ClassificationResult(
            is_finance_related=result["is_finance_related"],
            is_cheating=result["is_cheating"],
            intent=IntentType(result["intent"]),   # 문자열 → Enum 변환
        )