from ai.schemas.state import ChatContextState


class LangGraphClient:
    async def generate_answer(self, state: ChatContextState) -> dict:
        return {
            "route": "LLM",
            "intent": "OTHER",
            "hint_level": 1,
            "answer": f"현재는 mock LangGraph 응답입니다. 입력 메시지: {state.message}",
        }