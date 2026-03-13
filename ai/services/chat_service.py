from uuid import UUID

from ai.clients.langgraph_client import LangGraphClient
from ai.schemas.policy import PolicyDecision, PolicyReason
from ai.schemas.request import ChatRequest
from ai.schemas.response import ChatResponse
from ai.schemas.state import ChatContextState
from ai.services.intent_service import IntentService
from ai.services.policy_service import PolicyService


class ChatService:
    def __init__(self) -> None:
        self.langgraph_client = LangGraphClient()
        self.policy_service = PolicyService()
        self.intent_service = IntentService()

    async def handle_message(self, request: ChatRequest) -> ChatResponse:
        state = ChatContextState(
            user_id=UUID("11111111-1111-1111-1111-111111111111"),
            quiz_id=request.quiz_id,
            message=request.message,
            credits_balance=100,
            credits_spent_total=0,
            difficulty="medium",
            age_group="elementary",
        )

        # 1. credit pre-check
        policy_result = self.policy_service.run_policy_gate(state)
        state.policy_decision = policy_result.decision
        state.policy_reason = policy_result.reason

        if not policy_result.allowed:
            return ChatResponse(
                remaining_credits=state.credits_balance,
                ai_reply=policy_result.message or "",
            )

        # 2. LLM classification
        classification = self.intent_service.classify_message(state.message)
        state.is_finance_related = classification.is_finance_related
        state.is_cheating = classification.is_cheating
        state.intent = classification.intent.value

        # 3. semantic policy deny
        if state.is_cheating:
            state.policy_decision = PolicyDecision.DENY
            state.policy_reason = PolicyReason.CHEATING
            return ChatResponse(
                remaining_credits=state.credits_balance,
                ai_reply="정답은 직접 제공할 수 없습니다.",
            )

        if not state.is_finance_related:
            state.policy_decision = PolicyDecision.DENY
            state.policy_reason = PolicyReason.OUT_OF_SCOPE
            return ChatResponse(
                remaining_credits=state.credits_balance,
                ai_reply="금융 관련 질문만 가능합니다.",
            )

        # 4. LangGraph 실행
        result = await self.langgraph_client.generate_answer(state)

        state.route = result["route"]
        state.hint_level = result["hint_level"]

        return ChatResponse(
            remaining_credits=state.credits_balance,
            ai_reply=result["answer"],
        )