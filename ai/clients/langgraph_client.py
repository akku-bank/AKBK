# ai/clients/langgraph_client.py
# LangGraphClient는 상태 객체와 workflow 실행 함수를 받아
# LangGraph 실행 계층을 감싸는 클라이언트 역할만 담당한다.
# 현재는 실제 LangGraph 연동 전 단계이므로 전달받은 workflow를 그대로 실행한다.

from collections.abc import Awaitable, Callable
from typing import Any

from ai.schemas.state import ChatContextState


class LangGraphClient:
    async def run_workflow(
        self,
        state: ChatContextState,
        workflow: Callable[[ChatContextState], Awaitable[dict[str, Any]]],
    ) -> dict[str, Any]:
        return await workflow(state)
