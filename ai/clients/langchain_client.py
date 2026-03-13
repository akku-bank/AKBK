from typing import Any

from ai.core.config import Settings


class LangChainClient:
    # 현재 프로젝트에서 사용하는 모델 설정은 코드 상수로 고정합니다.
    EMBEDDING_MODEL = "text-embedding-3-small"
    CHAT_MODEL = "gpt-4o-mini"
    TEMPERATURE = 0.2
    MAX_TOKENS = 512

    def __init__(self) -> None:
        # 실제 LangChain 객체 생성은 지연 초기화로 처리합니다.
        # 패키지가 설치되지 않은 환경에서도 모듈 import 자체는 가능하게 두고,
        # 실제 사용 시점에만 명확한 오류를 내도록 합니다.
        self._embedding_client: Any | None = None
        self._chat_client: Any | None = None
        self._prompt_template: Any | None = None

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

    def build_prompt(self, question: str, contexts: list[str]) -> str:
        # 검색된 문서를 번호 목록으로 정리한 뒤, 질문과 함께 최종 프롬프트를 생성합니다.
        normalized_question = question.strip()
        if not normalized_question:
            raise ValueError("프롬프트에 사용할 question은 비어 있을 수 없습니다.")

        prompt_template = self._get_prompt_template()
        context_block = self._build_context_block(contexts)
        return prompt_template.format(question=normalized_question, context=context_block)

    async def call_llm(self, prompt: str) -> str:
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

    def _build_context_block(self, contexts: list[str]) -> str:
        # 검색 결과가 없더라도 프롬프트 규칙상 문맥 부재를 명시해 모델 응답을 안정화합니다.
        normalized_contexts = [context.strip() for context in contexts if context and context.strip()]
        if not normalized_contexts:
            return "참고할 컨텍스트가 없습니다."

        return "\n\n".join(
            f"[문서 {index}]\n{context}"
            for index, context in enumerate(normalized_contexts, start=1)
        )

    def _get_embedding_client(self) -> Any:
        # LangChain OpenAI 임베딩 클라이언트를 최초 1회만 생성합니다.
        if self._embedding_client is None:
            embeddings_cls = self._import_openai_embeddings()
            self._embedding_client = embeddings_cls(
                model=self.EMBEDDING_MODEL,
                api_key=Settings.require_openai_api_key(),
                base_url=Settings.OPENAI_BASE_URL,
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
                api_key=Settings.require_openai_api_key(),
                base_url=Settings.OPENAI_BASE_URL,
            )
        return self._chat_client

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

    def _import_openai_embeddings(self) -> Any:
        # 의존성이 없을 때는 설치가 필요하다는 메시지를 명확히 전달합니다.
        try:
            from langchain_openai import OpenAIEmbeddings
        except ImportError as exc:
            raise RuntimeError(
                "langchain_openai 패키지가 필요합니다. "
                "LangChainClient를 사용하려면 관련 의존성을 설치해야 합니다."
            ) from exc
        return OpenAIEmbeddings

    def _import_chat_openai(self) -> Any:
        # 챗 모델 래퍼도 동일하게 지연 import로 로드합니다.
        try:
            from langchain_openai import ChatOpenAI
        except ImportError as exc:
            raise RuntimeError(
                "langchain_openai 패키지가 필요합니다. "
                "LangChainClient를 사용하려면 관련 의존성을 설치해야 합니다."
            ) from exc
        return ChatOpenAI

    def _import_prompt_template(self) -> Any:
        # 프롬프트 템플릿도 LangChain core 의존성으로 분리되어 있을 수 있습니다.
        try:
            from langchain_core.prompts import PromptTemplate
        except ImportError as exc:
            raise RuntimeError(
                "langchain_core 패키지가 필요합니다. "
                "LangChainClient를 사용하려면 관련 의존성을 설치해야 합니다."
            ) from exc
        return PromptTemplate
