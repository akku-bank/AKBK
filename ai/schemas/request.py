from datetime import date
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    event_type: Literal["CHAT_REQUEST"]
    event_id: UUID
    user_id: UUID
    quiz_id: UUID
    message: str = Field(min_length=1, max_length=500)
    remaining_credits: int = Field(ge=0)
    difficulty: Literal["easy", "medium", "hard"]
    birth_date: date
    chat_json: str | None = None
