from pydantic import BaseModel, Field

class ChatResponse(BaseModel):
    remaining_credits: int = Field(alias="remainingCredits")
    ai_reply: str = Field(alias="aiReply")

    model_config = {
        "populate_by_name": True
    }
