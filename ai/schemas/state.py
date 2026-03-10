from pydantic import BaseModel
from typing import Literal
from uuid import UUID


class ChatContextState(BaseModel):
    user_id: UUID
    quiz_id: UUID
    message: str

    credits_balance: int
    credits_spent_total: int
    difficulty: Literal["easy", "medium", "hard"]
    age_group: Literal["preschool", "elementary", "middle", "high"]

    intent: Literal["DEFINE", "HINT", "EXPLAIN", "OTHER"] | None = None
    route: Literal["DICT", "RAG", "LLM", "DENY"] | None = None
    hint_level: int | None = None