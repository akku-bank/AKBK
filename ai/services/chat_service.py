from uuid import UUID

from ai.clients.langgraph_client import LangGraphClient
from ai.schemas.request import ChatRequest
from ai.schemas.response import ChatResponse
from ai.schemas.state import ChatContextState


class ChatService:
    def __init__(self) -> None:
        self.langgraph_client = LangGraphClient()

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

        result = await self.langgraph_client.generate_answer(state)

        return ChatResponse(
            route=result["route"],
            intent=result["intent"],
            hint_level=result["hint_level"],
            remaining_credits=state.credits_balance,
            answer=result["answer"],
        )