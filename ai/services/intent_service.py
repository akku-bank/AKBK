from ai.clients.llm_client import LLMClient
from ai.schemas.intent import ClassificationResult, IntentType


class IntentService:
    def __init__(self) -> None:
        self.llm_client = LLMClient()

    def classify_message(self, message: str) -> ClassificationResult:
        result = self.llm_client.classify_message(message)

        return ClassificationResult(
            is_finance_related=result["is_finance_related"],
            is_cheating=result["is_cheating"],
            intent=IntentType(result["intent"]),   # 문자열 → Enum 변환
        )