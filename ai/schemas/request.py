from pydantic import BaseModel, Field
from uuid import UUID


class ChatRequest(BaseModel):
    quiz_id: UUID = Field(alias="quizId")
    message: str = Field(alias="userMessage", min_length=1, max_length=500)

    model_config = {
        "populate_by_name": True
    }