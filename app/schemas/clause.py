from uuid import UUID
from typing import Literal

from pydantic import BaseModel


class ClauseBase(BaseModel):
    category: str
    original_text: str
    risk_level: Literal["LOW", "MEDIUM", "HIGH"]
    explanation: str
    negotiation_strategy: str


class ClauseCreate(ClauseBase):
    clause_number: int | None = None


class ClauseResponse(ClauseBase):
    id: UUID
    contract_id: UUID
    clause_number: int | None

    model_config = {"from_attributes": True}