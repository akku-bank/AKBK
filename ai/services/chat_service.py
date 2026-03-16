# ai/services/chat_service.py
# ChatService는 채팅 API 요청을 받아 응답 오케스트레이션 전체를 수행하는 진입 서비스다.
# README 기준에 맞춰 정책 검사, 의도 분류, 라우팅, Guard 재작성 루프를 서비스 계층에서 담당한다.

import logging
from datetime import date
from typing import Any

from ai.clients.langgraph_client import LangGraphClient
from ai.clients.llm_client import LLMClient
from ai.schemas.guard import GuardReason
from ai.schemas.policy import PolicyDecision, PolicyReason
from ai.schemas.request import ChatRequest
from ai.schemas.response import ChatResponse
from ai.schemas.state import ChatContextState
from ai.services.dictionary_service import DictionaryService
from ai.services.guard_service import GuardService
from ai.services.intent_service import IntentService
from ai.services.policy_service import PolicyService
from ai.services.rag_service import RagService
from ai.services.routing_service import RoutingService


logger = logging.getLogger(__name__)


class ChatService:
    MAX_GUARD_REWRITE_ATTEMPTS = 2
    CHEATING_DENY_MESSAGE = "정답을 직접 알려드릴 수는 없습니다."
    OUT_OF_SCOPE_DENY_MESSAGE = "금융 관련 질문만 도와드릴 수 있습니다."
    CREDIT_LIMIT_DENY_MESSAGE = "사용 가능한 크레딧을 모두 사용했습니다. 정답을 제출해 주세요."
    GENERIC_DENY_MESSAGE = "이 요청은 처리할 수 없습니다."
    GUARD_FALLBACK_MESSAGE = "더 안전한 방식으로만 힌트를 드릴 수 있습니다. 질문을 다시 표현해 주세요."

    def __init__(self) -> None:
        # 외부 workflow 실행은 client가 맡고, 실제 비즈니스 흐름은 ChatService가 담당한다.
        self.langgraph_client = LangGraphClient()
        self.policy_service = PolicyService()
        self.intent_service = IntentService()
        self.routing_service = RoutingService()
        self.dictionary_service = DictionaryService()
        self.rag_service = RagService()
        self.guard_service = GuardService()
        self.llm_client = LLMClient()

    async def handle_message(self, request: ChatRequest) -> ChatResponse:
        logger.info(
            "chat.request_received event_id=%s user_id=%s quiz_id=%s message=%s",
            request.event_id,
            request.user_id,
            request.quiz_id,
            request.message,
        )

        # 외부 요청 스키마를 내부 상태 객체로 변환한다.
        state = ChatContextState(
            user_id=request.user_id,
            quiz_id=request.quiz_id,
            message=request.message,
            credits_balance=request.remaining_credits,
            credits_spent_total=0,
            difficulty=request.difficulty,
            age_group=self._to_age_group(request.birth_date),
        )

        # 크레딧 기반 선검사는 오케스트레이션 전에 먼저 수행한다.
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
            return self._build_response(
                request=request,
                ai_reply=policy_result.message or "",
                deducted_credits=0,
            )

        try:
            result = await self.langgraph_client.run_workflow(state, self._run_workflow)
        except Exception:
            logger.exception("chat.orchestration_failed event_id=%s", request.event_id)
            return self._build_response(
                request=request,
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
        return self._build_response(
            request=request,
            ai_reply=result["answer"],
            deducted_credits=deducted_credits,
        )

    async def _run_workflow(self, state: ChatContextState) -> dict[str, Any]:
        logger.info(
            "langgraph.start quiz_id=%s policy_decision=%s policy_reason=%s",
            state.quiz_id,
            state.policy_decision,
            state.policy_reason,
        )

        if state.policy_decision == PolicyDecision.DENY:
            state.route = "DENY"
            state.hint_level = 1
            logger.info(
                "langgraph.short_circuit route=%s policy_reason=%s",
                state.route,
                state.policy_reason,
            )
            return self._format_result(
                route="DENY",
                hint_level=state.hint_level,
                answer=self._deny_message(state.policy_reason),
            )

        self._classify_if_needed(state)

        if state.is_cheating:
            state.policy_decision = PolicyDecision.DENY
            state.policy_reason = PolicyReason.CHEATING
            state.route = "DENY"
            state.hint_level = 1
            logger.info(
                "langgraph.semantic_deny route=%s policy_reason=%s",
                state.route,
                state.policy_reason,
            )
            return self._format_result(
                route="DENY",
                hint_level=state.hint_level,
                answer=self._deny_message(state.policy_reason),
            )

        if state.is_finance_related is False:
            state.policy_decision = PolicyDecision.DENY
            state.policy_reason = PolicyReason.OUT_OF_SCOPE
            state.route = "DENY"
            state.hint_level = 1
            logger.info(
                "langgraph.semantic_deny route=%s policy_reason=%s",
                state.route,
                state.policy_reason,
            )
            return self._format_result(
                route="DENY",
                hint_level=state.hint_level,
                answer=self._deny_message(state.policy_reason),
            )

        state.hint_level = self._calculate_hint_level(state)
        state.route = self.routing_service.select_route(
            state,
            rag_eligible=self._rag_eligible(state),
        )
        logger.info(
            "langgraph.route_selected intent=%s finance_related=%s cheating=%s route=%s hint_level=%s",
            state.intent,
            state.is_finance_related,
            state.is_cheating,
            state.route,
            state.hint_level,
        )

        answer = await self._execute_route(state)
        logger.info(
            "langgraph.route_executed route=%s answer_length=%s",
            state.route,
            len(answer),
        )

        if state.route in {"DICT", "DENY"}:
            return self._format_result(
                route=state.route,
                hint_level=state.hint_level,
                answer=answer,
            )

        guarded_answer = await self._apply_guard_with_rewrites(state=state, answer=answer)
        return self._format_result(
            route=state.route,
            hint_level=state.hint_level,
            answer=guarded_answer,
        )

    def _classify_if_needed(self, state: ChatContextState) -> None:
        # 상위 단계에서 분류가 이미 끝났다면 중복 호출하지 않는다.
        if (
            state.intent is not None
            and state.is_finance_related is not None
            and state.is_cheating is not None
        ):
            return

        classification = self.intent_service.classify_message(state.message)
        state.is_finance_related = classification.is_finance_related
        state.is_cheating = classification.is_cheating
        state.intent = classification.intent.value
        logger.info(
            "langgraph.intent_classified intent=%s finance_related=%s cheating=%s",
            state.intent,
            state.is_finance_related,
            state.is_cheating,
        )

    def _calculate_hint_level(self, state: ChatContextState) -> int:
        hint_level = 1

        if state.difficulty == "hard":
            hint_level += 1

        if state.credits_spent_total >= 100:
            hint_level += 1

        return min(hint_level, 3)

    def _rag_eligible(self, state: ChatContextState) -> bool | None:
        if state.intent == "EXPLAIN":
            return False
        return None

    async def _execute_route(self, state: ChatContextState) -> str:
        # RAG 실행 오케스트레이션은 RagService가 담당하고, ChatService는 분기만 결정한다.
        if state.route == "DENY":
            return self._deny_message(state.policy_reason)

        if state.route == "DICT":
            terms = self._extract_dictionary_terms(state.message)
            logger.info("langgraph.dictionary_lookup terms=%s", terms)
            result = self.dictionary_service.generate_answer(terms)
            return str(result["answer"])

        if state.route == "RAG":
            result = await self.rag_service.run_rag(
                state.message,
                user_id=state.user_id,
                quiz_id=state.quiz_id,
            )
            return str(result["answer"])

        if state.route == "LLM":
            prompt = self._build_llm_prompt(state)
            return await self.llm_client.generate_text(prompt)

        raise ValueError(f"Unsupported route: {state.route}")

    async def _apply_guard_with_rewrites(
        self,
        *,
        state: ChatContextState,
        answer: str,
    ) -> str:
        current_answer = answer
        guard_result = self.guard_service.check_output(current_answer)
        logger.info(
            "langgraph.guard_checked route=%s allowed=%s reason=%s",
            state.route,
            guard_result.allowed,
            guard_result.reason,
        )
        if guard_result.allowed:
            return current_answer

        for attempt in range(1, self.MAX_GUARD_REWRITE_ATTEMPTS + 1):
            logger.info(
                "langgraph.guard_rewrite_attempt route=%s attempt=%s reason=%s",
                state.route,
                attempt,
                guard_result.reason,
            )
            current_answer = await self._rewrite_answer(
                state=state,
                answer=current_answer,
                guard_reason=guard_result.reason,
                guard_message=guard_result.message,
            )
            guard_result = self.guard_service.check_output(current_answer)
            logger.info(
                "langgraph.guard_rewrite_checked route=%s attempt=%s allowed=%s reason=%s",
                state.route,
                attempt,
                guard_result.allowed,
                guard_result.reason,
            )
            if guard_result.allowed:
                return current_answer

        logger.warning(
            "langgraph.guard_fallback route=%s reason=%s",
            state.route,
            guard_result.reason,
        )
        return self.GUARD_FALLBACK_MESSAGE

    async def _rewrite_answer(
        self,
        *,
        state: ChatContextState,
        answer: str,
        guard_reason: GuardReason,
        guard_message: str | None,
    ) -> str:
        prompt = f"""
너는 금융 퀴즈 도우미의 답변을 더 안전하게 다시 작성하는 역할이다.

규칙:
- 정답을 직접 드러내지 않는다.
- 1번, 2번, 3번, 4번처럼 보기 번호를 언급하지 않는다.
- 너무 직설적이지 않게 우회적으로 설명한다.
- 연령대에 맞는 쉬운 표현을 사용한다.
- 교육적으로 도움이 되는 방향은 유지한다.

사용자 질문:
{state.message}

현재 답변:
{answer}

Guard 실패 사유:
{guard_reason.value}

Guard 메시지:
{guard_message or ""}

위 조건을 반영해서 한국어 답변만 다시 작성해라.
""".strip()
        return await self.llm_client.generate_text(prompt)

    def _build_llm_prompt(self, state: ChatContextState) -> str:
        return f"""
너는 어린이를 위한 금융 퀴즈 도우미다.

규칙:
- 정답을 직접 말하지 않는다.
- 보기 번호를 직접 언급하지 않는다.
- 나이에 맞는 쉬운 표현을 사용한다.
- 질문 의도에 맞게 짧고 분명하게 답한다.
- 힌트나 설명은 학습에 도움이 되도록 작성한다.

age_group={state.age_group}
hint_level={state.hint_level}
intent={state.intent}

사용자 질문:
{state.message}
""".strip()

    def _extract_dictionary_terms(self, message: str) -> list[str]:
        lowered_message = message.lower()
        matched_terms: list[str] = []
        seen_terms: set[str] = set()

        for term in self.dictionary_service.definitions.keys():
            if term in lowered_message and term not in seen_terms:
                seen_terms.add(term)
                matched_terms.append(term)

        if matched_terms:
            return matched_terms

        return [message.strip()]

    def _deny_message(self, reason: PolicyReason | None) -> str:
        if reason == PolicyReason.CREDIT_LIMIT:
            return self.CREDIT_LIMIT_DENY_MESSAGE
        if reason == PolicyReason.CHEATING:
            return self.CHEATING_DENY_MESSAGE
        if reason == PolicyReason.OUT_OF_SCOPE:
            return self.OUT_OF_SCOPE_DENY_MESSAGE
        return self.GENERIC_DENY_MESSAGE

    def _to_age_group(self, birth_date: date) -> str:
        # 생년월일을 기준으로 내부 age_group 값을 계산한다.
        today = date.today()
        age = today.year - birth_date.year
        if (today.month, today.day) < (birth_date.month, birth_date.day):
            age -= 1

        if age <= 7:
            return "preschool"
        if age <= 13:
            return "elementary"
        if age <= 16:
            return "middle"
        return "high"

    def _calculate_deducted_credits(self, route: str | None) -> int:
        # route별 차감 크레딧 정책을 응답에 반영한다.
        if route == "DICT":
            return 5
        if route == "RAG":
            return 20
        if route == "LLM":
            return 10
        return 0

    @staticmethod
    def _format_result(*, route: str, hint_level: int, answer: str) -> dict[str, Any]:
        return {
            "route": route,
            "hint_level": hint_level,
            "answer": answer,
        }

    @staticmethod
    def _build_response(
        *,
        request: ChatRequest,
        ai_reply: str,
        deducted_credits: int,
    ) -> ChatResponse:
        return ChatResponse(
            event_type="CHAT_RESPONSE",
            event_id=request.event_id,
            user_id=request.user_id,
            quiz_id=request.quiz_id,
            message=request.message,
            ai_reply=ai_reply,
            deducted_credits=deducted_credits,
        )
