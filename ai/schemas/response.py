from pydantic import BaseModel
from typing import Literal

class ChatResponse(BaseModel):
    route: Literal["DICT", "RAG", "LLM", "DENY"]
    intent: Literal["DEFINE", "HINT", "EXPLAIN", "OTHER"]
    hint_lavel: int
    remaining_credits: int
    answer: str
    