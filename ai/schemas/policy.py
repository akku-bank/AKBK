from enum import Enum
from pydantic import BaseModel


class PolicyDecision(str, Enum):
    ALLOW = "ALLOW"
    DENY = "DENY"


class PolicyReason(str, Enum):
    NONE = "NONE"
    CREDIT_LIMIT = "CREDIT_LIMIT"
    OUT_OF_SCOPE = "OUT_OF_SCOPE"
    CHEATING = "CHEATING"


class PolicyResult(BaseModel):
    decision: PolicyDecision
    reason: PolicyReason = PolicyReason.NONE
    message: str | None = None

    @property
    def allowed(self) -> bool:
        return self.decision == PolicyDecision.ALLOW