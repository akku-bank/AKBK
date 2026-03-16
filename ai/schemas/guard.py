from enum import Enum
from pydantic import BaseModel


class GuardReason(str, Enum):
    NONE = "NONE"
    ANSWER_LEAK = "ANSWER_LEAK"
    CHOICE_NUMBER_MENTION = "CHOICE_NUMBER_MENTION"
    TOO_DIRECT = "TOO_DIRECT"


class GuardResult(BaseModel):
    allowed: bool
    reason: GuardReason = GuardReason.NONE
    message: str | None = None
