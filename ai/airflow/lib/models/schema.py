from typing import List, Optional, TypedDict
from pydantic import BaseModel, Field

class QuizOutput(BaseModel):
    topic: str = Field(description="퀴즈의 핵심 주제")
    question: str = Field(description="퀴즈 문제 내용")
    options: List[str] = Field(description="4개의 선택지 리스트", min_items=4, max_items=4)
    correct_choice_no: int = Field(description="정답 번호 (1-4)", ge=1, le=4)
    explanation: str = Field(description="정답에 대한 상세 설명")

class QuizState(TypedDict):
    difficulty: str
    quiz_data: Optional[QuizOutput]
    error_history: List[str]
    retry_count: int
    is_valid: bool