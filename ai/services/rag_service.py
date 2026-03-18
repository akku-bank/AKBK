# ai/services/rag_service.py
# RagService는 RAG 오케스트레이션을 담당하는 서비스 계층입니다.
from typing import Any
from uuid import UUID

from ai.clients.llm_client import LLMClient
from ai.clients.vector_db_client import VectorDbClient


class RagService:
    def __init__(self) -> None:
        # LLM/임베딩 담당 클라이언트
        self.llm_client = LLMClient()
        # 벡터DB 조회 담당 클라이언트
        self.vector_db_client = VectorDbClient()
        # 프롬프트 템플릿 캐싱
        self._prompt_template: Any | None = None

    async def run_rag(
        self,
        question: str,
        *,
        user_id: UUID | None = None,
        quiz_id: UUID | None = None,
    ) -> dict[str, Any]:
        # RAG 오케스트레이션: 임베딩 -> 벡터검색 -> 컨텍스트선택 -> 프롬프트 -> 답변생성
        query = self.build_query(question)
        query_embedding = await self.llm_client.create_query_embedding(query)
        docs = await self.retrieve_contexts(query_embedding=query_embedding, top_k=5)
        selected_docs = self.select_contexts(docs=docs, max_docs=3)
        prompt = self.build_prompt(question=question, contexts=selected_docs)
        answer = await self.generate_answer(prompt=prompt)
        return self.format_result(answer=answer, contexts=selected_docs)

    def build_query(self, question: str) -> str:
        # 검색 품질을 위해 공백 정리
        return question.strip()

    async def retrieve_contexts(
        self,
        query_embedding: list[float],
        top_k: int = 5,
    ) -> list[dict[str, Any]]:
        # 벡터DB 클라이언트로 유사 문서 검색
        return await self.vector_db_client.search_similar_by_embedding(
            query_embedding=query_embedding,
            top_k=top_k,
        )

    def select_contexts(
        self,
        docs: list[dict[str, Any]],
        max_docs: int = 3, # 상위 3개 문서만 선택 (추후 점수 기준 재정렬 가능)
    ) -> list[dict[str, Any]]:
        return docs[:max_docs]

    def build_prompt(self, question: str, contexts: list[dict[str, Any]]) -> str:
        # 문서 목록에서 본문만 뽑아 프롬프트 조합
        context_texts = [doc.get("content", "") for doc in contexts]
        normalized_question = question.strip()
        if not normalized_question:
            raise ValueError("프롬프트에 사용할 question은 비어 있을 수 없습니다.")

        prompt_template = self._get_prompt_template()
        context_block = self._build_context_block(context_texts)
        return prompt_template.format(question=normalized_question, context=context_block)

    async def generate_answer(self, prompt: str) -> str:
        # 최종 프롬프트로 LLM 호출
        return await self.llm_client.generate_text(prompt=prompt)

    def format_result(self, answer: str, contexts: list[dict[str, Any]]) -> dict[str, Any]:
        # 상위 계층에서 사용하기 쉬운 응답 형태로 반환
        return {
            "route": "RAG",
            "answer": answer,
            "contexts": contexts,
        }

    def fallback_answer(self, question: str, reason: str) -> dict[str, Any]:
        # 검색/생성 실패 시 기본 응답
        return {
            "route": "RAG",
            "answer": f"질문에 대한 답변을 생성하지 못했습니다. reason={reason}",
            "contexts": [],
            "question": question,
        }

    def _build_context_block(self, contexts: list[str]) -> str:
        # 검색 결과가 없더라도 프롬프트 규칙상 문맥 부재를 명시해 모델 응답을 안정화합니다.
        normalized_contexts = [context.strip() for context in contexts if context and context.strip()]
        if not normalized_contexts:
            return "참고할 컨텍스트가 없습니다."

        return "\n\n".join(
            f"[문서 {index}]\n{context}"
            for index, context in enumerate(normalized_contexts, start=1)
        )
    
    def _get_prompt_template(self) -> Any:
        # PromptTemplate를 재사용해서 프롬프트 규칙을 한 곳에서 관리합니다.
        if self._prompt_template is None:
            prompt_cls = self._import_prompt_template()
            self._prompt_template = prompt_cls.from_template(
                """
당신은 어린이 금융 학습을 돕는 AI 튜터입니다.

아래 규칙을 반드시 지켜 답변하세요.
1. 답변은 한국어로 작성합니다.
2. 제공된 컨텍스트를 가장 우선적으로 사용합니다.
3. 컨텍스트에 근거가 없으면 추측하지 말고 모른다고 답변합니다.
4. 어린이가 이해할 수 있도록 쉬운 표현을 사용합니다.

[컨텍스트]
{context}

[질문]
{question}

[답변]
                """.strip()
            )
        return self._prompt_template

    def _import_prompt_template(self) -> Any:
        # 프롬프트 템플릿도 LangChain core 의존성으로 분리되어 있을 수 있습니다.
        try:
            from langchain_core.prompts import PromptTemplate
        except ImportError as exc:
            raise RuntimeError(
                "langchain_core 패키지가 필요합니다. "
                "RagService를 사용하려면 관련 의존성을 설치해야 합니다."
            ) from exc
        return PromptTemplate
