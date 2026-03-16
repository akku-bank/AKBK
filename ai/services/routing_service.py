# ai/services/routing_service.py
# RoutingService는 의도 분류와 정책 검사 결과를 바탕으로 다음 실행 경로(DICT / RAG / LLM / DENY)를 결정하는 서비스 계층입니다.
# 라우팅 로직은 현재 간단한 규칙 기반으로 구현되어 있습니다.
from ai.schemas.policy import PolicyDecision
from ai.schemas.state import ChatContextState


class RoutingService:
    # 라우팅 서비스는 의도 분류와 정책 검사 결과를 바탕으로
    # 다음 실행 경로(DICT / RAG / LLM / DENY)만 결정합니다.
    def select_route(
        self,
        state: ChatContextState,
        *,
        rag_eligible: bool | None = None,
    ) -> str:
        # 상위 단계에서 이미 정책 차단이 결정된 경우에는
        # 이후 실행 노드로 보내지 않고 즉시 DENY로 고정합니다.
        if state.policy_decision == PolicyDecision.DENY:
            return "DENY"

        # intent가 비어 있으면 정상적인 라우팅을 할 수 없습니다.
        if state.intent is None:
            raise ValueError("route를 결정하려면 state.intent가 필요합니다.")

        # 용어 정의 요청은 사전 기반 응답으로 보냅니다.
        if state.intent == "DEFINE":
            return "DICT"

        # 힌트 요청은 퀴즈 맥락 기반 근거 응답이 필요하므로 RAG로 보냅니다.
        if state.intent == "HINT":
            return "RAG"

        # 설명 요청은 내부 코퍼스 검색 결과에 따라 RAG/LLM을 나눕니다.
        # rag_eligible이 아직 계산되지 않았다면 상위 오케스트레이션에서 먼저 검색 판단 단계를 수행해야 하므로 예외를 발생시킵니다.
        # (추후 랭그래프 연동 시, rag_eligible 대신 검색 결과 유무나 점수 기준으로 판단하도록 수정할 수 있습니다.)
        if state.intent == "EXPLAIN":
            if rag_eligible is None:
                raise ValueError("EXPLAIN route를 결정하려면 rag_eligible 값이 필요합니다.")
            return "RAG" if rag_eligible else "LLM"

        # 나머지 일반 대화는 기본 LLM 응답으로 보냅니다.
        return "LLM"

    def apply_route(
        self,
        state: ChatContextState,
        *,
        rag_eligible: bool | None = None,
    ) -> ChatContextState:
        # 라우팅 결과를 state에 반영한 뒤 다음 노드로 넘길 수 있게 합니다.
        state.route = self.select_route(state, rag_eligible=rag_eligible)
        return state
