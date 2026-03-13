import json
from openai import OpenAI

from ai.core.config import settings


class LLMClient:
    def __init__(self) -> None:
        self.client = OpenAI(api_key=settings.openai_api_key)
        self.model = settings.openai_model

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