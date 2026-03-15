import json
from typing import Any

from openai import OpenAI
from ai.core.config import settings


class LLMClient:
    # 현재 프로젝트에서 사용하는 모델 설정은 코드 상수로 고정합니다.
    EMBEDDING_MODEL = "text-embedding-3-small"
    CHAT_MODEL = "gpt-4o-mini"
    TEMPERATURE = 0.2
    MAX_TOKENS = 512

    def __init__(self) -> None:
        self.client = OpenAI(
            api_key=settings.require_openai_api_key(),
            base_url=settings.openai_base_url
        )
        self.model = settings.openai_model

        # Rag용 LangChain 객체는 지연 초기화
        self._embedding_client: Any | None = None
        self._chat_client: Any | None = None        

    def classify_message(self, message: str) -> dict:
        system_prompt = """
당신은 어린이 금융 퀴즈 챗봇의 질문 분류기입니다.

사용자 메시지를 보고 아래 JSON 형식으로만 응답하세요.

{
  "is_finance_related": true 또는 false,
  "is_cheating": true 또는 false,
  "intent": "DEFINE" | "HINT" | "EXPLAIN" | "OTHER"
}

판단 기준:
- is_finance_related:
  금융/경제/저축/투자/이자/퀴즈 풀이 맥락이면 true
  완전히 무관한 일반 대화면 false

- is_cheating:
  정답 직접 요청, 답/보기 번호 요청, 답만 달라는 요청이면 true
  힌트 요청이나 개념 설명 요청은 false

- intent:
  DEFINE: 용어 뜻/정의 요청
  HINT: 문제 풀이 힌트 요청
  EXPLAIN: 개념 설명, 이유 설명 요청
  OTHER: 그 외

반드시 JSON만 출력하세요.
"""

        response = self.client.chat.completions.create(
            model=self.model,
            temperature=0,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": message},
            ],
        )

        content = response.choices[0].message.content
        return json.loads(content)

    async def create_query_embedding(self, query: str) -> list[float]:
        # 검색용 질의를 임베딩 벡터로 변환합니다.
        normalized_query = query.strip()
        if not normalized_query:
            raise ValueError("임베딩할 query는 비어 있을 수 없습니다.")

        embedding_client = self._get_embedding_client()
        embedding = await embedding_client.aembed_query(normalized_query)

        if not embedding:
            raise RuntimeError("질문 임베딩 생성 결과가 비어 있습니다.")

        return [float(value) for value in embedding]

    async def generate_text(self, prompt: str) -> str:
        # 완성된 프롬프트를 챗 모델에 전달하고 문자열 답변만 추출합니다.
        normalized_prompt = prompt.strip()
        if not normalized_prompt:
            raise ValueError("LLM에 전달할 prompt는 비어 있을 수 없습니다.")

        chat_client = self._get_chat_client()
        response = await chat_client.ainvoke(normalized_prompt)
        content = getattr(response, "content", "")

        if isinstance(content, str):
            answer = content.strip()
        elif isinstance(content, list):
            # 일부 LangChain 응답은 content를 블록 리스트로 돌려주므로 텍스트만 합칩니다.
            answer = "".join(
                block.get("text", "")
                for block in content
                if isinstance(block, dict)
            ).strip()
        else:
            answer = str(content).strip()

        if not answer:
            raise RuntimeError("LLM 응답에서 답변 텍스트를 추출하지 못했습니다.")

        return answer

    def _get_embedding_client(self) -> Any:
        # LangChain OpenAI 임베딩 클라이언트를 최초 1회만 생성합니다.
        if self._embedding_client is None:
            embeddings_cls = self._import_openai_embeddings()
            self._embedding_client = embeddings_cls(
                model=self.EMBEDDING_MODEL,
                api_key=settings.require_openai_api_key(),
                base_url=settings.openai_base_url,
            )
        return self._embedding_client

    def _get_chat_client(self) -> Any:
        # LangChain 챗 모델 클라이언트를 최초 1회만 생성합니다.
        if self._chat_client is None:
            chat_cls = self._import_chat_openai()
            self._chat_client = chat_cls(
                model=self.CHAT_MODEL,
                temperature=self.TEMPERATURE,
                max_tokens=self.MAX_TOKENS,
                api_key=settings.require_openai_api_key(),
                base_url=settings.openai_base_url,
            )
        return self._chat_client

    def _import_openai_embeddings(self) -> Any:
        # 의존성이 없을 때는 설치가 필요하다는 메시지를 명확히 전달합니다.
        try:
            from langchain_openai import OpenAIEmbeddings
        except ImportError as exc:
            raise RuntimeError(
                "langchain_openai 패키지가 필요합니다. "
                "LLMClient를 사용하려면 관련 의존성을 설치해야 합니다."
            ) from exc
        return OpenAIEmbeddings

    def _import_chat_openai(self) -> Any:
        # 챗 모델 래퍼도 동일하게 지연 import로 로드합니다.
        try:
            from langchain_openai import ChatOpenAI
        except ImportError as exc:
            raise RuntimeError(
                "langchain_openai 패키지가 필요합니다. "
                "LLMClient를 사용하려면 관련 의존성을 설치해야 합니다."
            ) from exc
        return ChatOpenAI
