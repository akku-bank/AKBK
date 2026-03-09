from pydantic import BaseModel, Field
from uuid import UUID


class ChatRequest(BaseModel):
    quiz_id: UUID
    message: str = Field(..., min_length=1, max_length=500)