from datetime import datetime
from uuid import UUID

from pydantic import BaseModel


class ContractCreate(BaseModel):
    filename: str
    raw_text: str
    document_hash: str


class ContractResponse(BaseModel):
    id: UUID
    document_hash: str
    filename: str
    risk_score: int | None
    created_at: datetime
    clauses: list["ClauseResponse"] = []

    model_config = {"from_attributes": True}


class AnalysisResult(BaseModel):
    document_hash: str
    risk_score: int
    clauses: list[ContractCreate]
    is_cached: bool = False