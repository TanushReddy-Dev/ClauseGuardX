import json
from uuid import UUID

import pytest

from app.schemas.contract import AnalysisResult


#
# --- Ingest Endpoint Tests ---
#


@pytest.mark.asyncio
async def test_ingest_endpoint_first_upload(client, pdf_bytes):
    """First upload should return is_cached=False with a valid document_hash."""
    response = await client.post("/api/v1/contracts/ingest", files={"file": ("test.pdf", pdf_bytes, "application/pdf")})
    assert response.status_code == 201

    data = response.json()
    assert "document_hash" in data
    assert UUID(data["document_hash"])  # valid UUID format
    assert data["is_cached"] is False
    assert "filename" in data
    assert "risk_score" in data
    assert "clauses" in data


@pytest.mark.asyncio
async def test_ingest_endpoint_duplicate_upload(client, pdf_bytes):
    """Second upload of same PDF should return is_cached=True."""
    # First upload
    response1 = await client.post("/api/v1/contracts/ingest", files={"file": ("test.pdf", pdf_bytes, "application/pdf")})
    assert response1.status_code == 201
    data1 = response1.json()
    assert data1["is_cached"] is False

    # Second upload of identical content
    response2 = await client.post("/api/v1/contracts/ingest", files={"file": ("test.pdf", pdf_bytes, "application/pdf")})
    assert response2.status_code == 200
    data2 = response2.json()
    assert data2["is_cached"] is True
    # The document_hash should be identical
    assert data1["document_hash"] == data2["document_hash"]


#
# --- Analyze Endpoint Tests (with mocked LLM) ---
#


@pytest.mark.asyncio
async def test_analyze_endpoint_mocked(client, pdf_bytes, mock_analyze_clause):
    """Analyze endpoint with mocked LLM should persist contract + clauses to DB."""
    response = await client.post("/api/v1/contracts/ingest", files={"file": ("test.pdf", pdf_bytes, "application/pdf")})
    assert response.status_code == 201

    data = response.json()
    assert data["is_cached"] is False
    assert "document_hash" in data
    assert "risk_score" in data
    assert "filename" in data
    assert "clauses" in data

    # Query the test database to assert rows were persisted
    from app.db.session import get_async_session
    from app.db.base import async_engine
    from app.models.contract import Contract
    from app.models.clause import Clause

    async with get_async_session() as session:
        # Check contract exists
        result = await session.execute(select(Contract).where(Contract.document_hash == data["document_hash"]))
        contract = result.scalar_one_or_none()
        assert contract is not None
        assert contract.filename == "test.pdf"

        # Check clauses exist
        clause_result = await session.execute(select(Clause).where(Clause.contract_id == contract.id))
        clauses = clause_result.scalars().all()
        assert len(clauses) > 0, f"Expected clauses to be persisted, found {len(clauses)}"
        # Verify clause fields match the mocked data
        for clause in clauses:
            assert clause.category == mock_analyze_clause.category
            assert clause.risk_level == mock_analyze_clause.risk_level
            assert clause.explanation == mock_analyze_clause.explanation
            assert clause.negotiation_strategy == mock_analyze_clause.negotiation_strategy


#
# --- Analysis Result Schema Validation ---
#


def test_analysis_result_schema():
    """Assert the AnalysisResult Pydantic model validates correctly."""
    from app.schemas.contract import AnalysisResult

    data = {
        "document_hash": "b4e1d7f0e7a7e4c3b2a1f6d5c4a3b2d1e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0",
        "risk_score": 5,
        "clauses": [
            {
                "id": "c1d2e3f4-5678-90ab-cdef-1234567890ab",
                "category": "Test",
                "original_text": "dummy text",
                "risk_level": "LOW",
                "explanation": "Test clause",
                "negotiation_strategy": "Test strategy",
                "clause_number": 1,
            }
        ],
        "is_cached": False,
    }

    result = AnalysisResult(**data)
    assert result.is_cached is False
    assert len(result.clauses) == 1
    assert result.clauses[0].risk_level == "LOW"