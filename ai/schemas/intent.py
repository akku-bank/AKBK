from enum import Enum

from pydantic import BaseModel


class IntentType(str, Enum):
    DEFINE = "DEFINE"
    HINT = "HINT"
    EXPLAIN = "EXPLAIN"
    OTHER = "OTHER"


class ClassificationResult(BaseModel):
    is_finance_related: bool
    is_cheating: bool
    intent: IntentType
