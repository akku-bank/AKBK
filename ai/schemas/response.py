from typing import Literal
from uuid import UUID

from pydantic import BaseModel


class ChatResponse(BaseModel):
    event_type: Literal["CHAT_RESPONSE"]
    event_id: UUID
    user_id: UUID
    quiz_id: UUID
    message: str
    ai_reply: str
    deducted_credits: int
    chat_json: str | None = None
