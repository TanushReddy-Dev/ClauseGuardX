import uuid
from datetime import datetime
from sqlalchemy import ForeignKey, Index, String, Integer, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


# Explicit Index objects for SQLAlchemy 2.0
ix_contract_document_hash = Index("ix_contracts_document_hash", "document_hash", unique=True, postgresql_ops={"document_hash": "text_pattern_ops"})
ix_clause_contract_id = Index("ix_clauses_contract_id", "contract_id")
ix_clause_category = Index("ix_clauses_category", "category")

class Contract(Base):
    __tablename__ = "contracts"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    document_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    filename: Mapped[str] = mapped_column(String(255), nullable=False)
    raw_text: Mapped[str] = mapped_column(nullable=False)
    risk_score: Mapped[int | None] = mapped_column(nullable=True)
    created_at: Mapped[datetime] = mapped_column(server_default=func.now())

    clauses = relationship("Clause", back_populates="contract", cascade="all, delete-orphan")


class Clause(Base):
    __tablename__ = "clauses"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    contract_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("contracts.id", ondelete="CASCADE"), nullable=False
    )
    clause_number: Mapped[int | None] = mapped_column(nullable=True)
    category: Mapped[str] = mapped_column(String(100), nullable=False)
    original_text: Mapped[str] = mapped_column(nullable=False)
    risk_level: Mapped[str] = mapped_column(String(20), nullable=False)
    explanation: Mapped[str] = mapped_column(nullable=False)
    negotiation_strategy: Mapped[str] = mapped_column(nullable=False)

    contract = relationship("Contract", back_populates="clauses")