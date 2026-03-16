# ai/services/chat_service.py
# ChatService는 채팅 API 요청을 받아 응답 오케스트레이션으로 연결하는 진입 서비스다.
# 이 계층은 state를 만들고 공통 선검사를 수행한 뒤, 실제 응답 생성은 LangGraphClient에 위임한다.

import logging
from uuid import UUID

from ai.clients.langgraph_client import LangGraphClient
from ai.schemas.request import ChatRequest
from ai.schemas.response import ChatResponse
from ai.schemas.state import ChatContextState
from ai.services.policy_service import PolicyService


logger = logging.getLogger(__name__)


class ChatService:
    def __init__(self) -> None:
        self.langgraph_client = LangGraphClient()
        self.policy_service = PolicyService()

    async def handle_message(self, request: ChatRequest) -> ChatResponse:
        logger.info("chat.request_received quiz_id=%s message=%s", request.quiz_id, request.message)

        state = ChatContextState(
            user_id=UUID("11111111-1111-1111-1111-111111111111"),
            quiz_id=request.quiz_id,
            message=request.message,
            credits_balance=100,
            credits_spent_total=0,
            difficulty="medium",
            age_group="elementary",
        )

        policy_result = self.policy_service.run_policy_gate(state)
        state.policy_decision = policy_result.decision
        state.policy_reason = policy_result.reason
        logger.info(
            "chat.policy_checked decision=%s reason=%s remaining_credits=%s",
            state.policy_decision,
            state.policy_reason,
            state.credits_balance,
        )

        if not policy_result.allowed:
            logger.info("chat.request_denied reason=%s", state.policy_reason)
            return ChatResponse(
                remaining_credits=state.credits_balance,
                ai_reply=policy_result.message or "",
            )

        try:
            result = await self.langgraph_client.generate_answer(state)
        except Exception:
            logger.exception("chat.orchestration_failed quiz_id=%s", request.quiz_id)
            return ChatResponse(
                remaining_credits=state.credits_balance,
                ai_reply="지금은 응답을 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.",
            )

        state.route = result["route"]
        state.hint_level = result["hint_level"]
        logger.info(
            "chat.response_ready route=%s hint_level=%s answer_length=%s",
            state.route,
            state.hint_level,
            len(result["answer"]),
        )
        return ChatResponse(
            remaining_credits=state.credits_balance,
            ai_reply=result["answer"],
        )
