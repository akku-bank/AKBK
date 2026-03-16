# ai/services/chat_service.py
# ChatService는 채팅 API 요청을 받아 응답 오케스트레이션으로 연결하는 진입 서비스다.

import logging
from datetime import date

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
        logger.info(
            "chat.request_received event_id=%s user_id=%s quiz_id=%s message=%s",
            request.event_id,
            request.user_id,
            request.quiz_id,
            request.message,
        )

        state = ChatContextState(
            user_id=request.user_id,
            quiz_id=request.quiz_id,
            message=request.message,
            credits_balance=request.remaining_credits,
            credits_spent_total=0,
            difficulty=request.difficulty,
            age_group=self._to_age_group(request.birth_date),
        )

        policy_result = self.policy_service.run_policy_gate(state)
        state.policy_decision = policy_result.decision
        state.policy_reason = policy_result.reason
        logger.info(
            "chat.policy_checked event_id=%s decision=%s reason=%s remaining_credits=%s",
            request.event_id,
            state.policy_decision,
            state.policy_reason,
            state.credits_balance,
        )

        if not policy_result.allowed:
            logger.info("chat.request_denied event_id=%s reason=%s", request.event_id, state.policy_reason)
            return ChatResponse(
                event_type="CHAT_RESPONSE",
                event_id=request.event_id,
                user_id=request.user_id,
                quiz_id=request.quiz_id,
                message=request.message,
                ai_reply=policy_result.message or "",
                deducted_credits=0,
            )

        try:
            result = await self.langgraph_client.generate_answer(state)
        except Exception:
            logger.exception("chat.orchestration_failed event_id=%s", request.event_id)
            return ChatResponse(
                event_type="CHAT_RESPONSE",
                event_id=request.event_id,
                user_id=request.user_id,
                quiz_id=request.quiz_id,
                message=request.message,
                ai_reply="지금은 응답을 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.",
                deducted_credits=0,
            )

        state.route = result["route"]
        state.hint_level = result["hint_level"]
        deducted_credits = self._calculate_deducted_credits(state.route)
        logger.info(
            "chat.response_ready event_id=%s route=%s hint_level=%s deducted_credits=%s answer_length=%s",
            request.event_id,
            state.route,
            state.hint_level,
            deducted_credits,
            len(result["answer"]),
        )
        return ChatResponse(
            event_type="CHAT_RESPONSE",
            event_id=request.event_id,
            user_id=request.user_id,
            quiz_id=request.quiz_id,
            message=request.message,
            ai_reply=result["answer"],
            deducted_credits=deducted_credits,
        )

    def _to_age_group(self, birth_date: date) -> str:
        today = date.today()
        age = today.year - birth_date.year
        if (today.month, today.day) < (birth_date.month, birth_date.day):
            age -= 1

        if age <= 10:
                return "초등학교 저학년 미만"
        if age <= 15:
            return "초등학교 고학년 ~ 중학생"
        else:
            return "고등학생 이상"

    def _calculate_deducted_credits(self, route: str | None) -> int:
        if route == "DICT":
            return 5
        if route == "RAG":
            return 20
        if route == "LLM":
            return 10
        return 0
