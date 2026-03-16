from uuid import UUID

from ai.clients.langgraph_client import LangGraphClient
from ai.schemas.request import ChatRequest
from ai.schemas.response import ChatResponse
from ai.schemas.state import ChatContextState
from ai.services.policy_service import PolicyService

class ChatService:
    def __init__(self) -> None:
        self.langgraph_client = LangGraphClient()
        self.policy_service = PolicyService()

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

        # 정책 검사
        policy_result = self.policy_service.run_policy_gate(state)

        state.policy_decision = policy_result.decision
        state.policy_reason = policy_result.reason

        # 정책 차단이면 바로 응답
        if not policy_result.allowed:
            return ChatResponse(
                remaining_credits=state.credits_balance,
                ai_reply=policy_result.message,
            )

        # 정책 통과 → LangGraph 실행
        result = await self.langgraph_client.generate_answer(state)

        # 내부 처리용 메타데이터 반영
        state.route = result["route"]
        state.intent = result["intent"]
        state.hint_level = result["hint_level"]

        # TODO:
        # - route / intent / hint_level 기반 로깅
        # - 크레딧 차감 처리
        # - 정책 판단 결과 저장
        # - chat_logs 저장

        return ChatResponse(
            remaining_credits=state.credits_balance,
            ai_reply=result["answer"],
        )