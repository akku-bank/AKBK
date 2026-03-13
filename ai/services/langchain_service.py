# ai/services/langchain_service.py
# LangChainService는 RAG 오케스트레이션을 담당하는 서비스 계층입니다.
from typing import Any
from uuid import UUID

from ai.clients.langchain_client import LangChainClient
from ai.clients.vector_db_client import VectorDbClient


class LangChainService:
    def __init__(self) -> None:
        # LLM/임베딩 담당 클라이언트
        self.langchain_client = LangChainClient()
        # 벡터DB 조회 담당 클라이언트
        self.vector_db_client = VectorDbClient()

    async def run_rag(
        self,
        question: str,
        *,
        user_id: UUID | None = None,
        quiz_id: UUID | None = None,
    ) -> dict[str, Any]:
        # RAG 오케스트레이션: 임베딩 -> 벡터검색 -> 컨텍스트선택 -> 프롬프트 -> 답변생성
        query = self.build_query(question)
        query_embedding = await self.langchain_client.create_query_embedding(query)
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
        return self.langchain_client.build_prompt(question=question, contexts=context_texts)

    async def generate_answer(self, prompt: str) -> str:
        # 최종 프롬프트로 LLM 호출
        return await self.langchain_client.call_llm(prompt=prompt)

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
