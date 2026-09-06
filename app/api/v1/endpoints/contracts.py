from fastapi import APIRouter, UploadFile, File, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, joinedload
from typing import Optional

from app.core.config import settings
from app.db.session import get_db
from app.models.contract import Contract
from app.models.clause import Clause
from app.schemas.contract import ContractResponse, AnalysisResult
from app.services.extractor import extract_text_from_pdf, compute_sha256
from app.services.cache import get_cached_analysis, set_cached_analysis
from app.services.pipeline import process_contract


router = APIRouter(prefix="/api/v1/contracts", tags=["contracts"])


@router.post("/ingest", response_model=AnalysisResult)
async def ingest_pdf(
    pdf_file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db),
):
    """Upload a PDF, extract text, hash it, check cache/DB, run AI pipeline.

    Returns an AnalysisResult with the full contract and all classified clauses.
    """
    file_bytes = await pdf_file.read()

    # 1. Extract raw text and compute SHA-256 hash
    raw_text = extract_text_from_pdf(file_bytes)
    doc_hash = compute_sha256(raw_text)

    # 2. Check Redis cache first
    cached = await get_cached_analysis(doc_hash)
    if cached is not None:
        return JSONResponse(
            content={**cached, "is_cached": True},
            status_code=200,
        )

    # 3. Check Postgres for existing contract with same document_hash
    stmt = select(Contract).where(Contract.document_hash == doc_hash)
    result = await db.execute(stmt)
    existing_contract = result.scalar_one_or_none()

    if existing_contract is not None:
        # Populate Redis from DB record
        cached_data = {
            "document_hash": existing_contract.document_hash,
            "risk_score": existing_contract.risk_score,
            "is_cached": True,
        }
        await set_cached_analysis(doc_hash, cached_data)
        # Return the full contract response with clauses
        return JSONResponse(
            content={
                "document_hash": existing_contract.document_hash,
                "risk_score": existing_contract.risk_score,
                "filename": existing_contract.filename,
                "clauses": existing_contract.clauses,  # type: ignore[attr-defined]
                "is_cached": True,
            },
            status_code=200,
        )

    # 4. Insert new Contract record
    new_contract = Contract(
        document_hash=doc_hash,
        filename=pdf_file.filename or "uploaded.pdf",
        raw_text=raw_text,
    )
    db.add(new_contract)
    await db.commit()
    await db.refresh(new_contract)

    # 5. Run the concurrent AI pipeline (chunk + analyze + persist)
    clause_models = await process_contract(new_contract.id, raw_text, db)

    # 6. Populate Redis cache
    # Build a lightweight dict for caching: document_hash + risk_score + is_cached
    # The full clause list will be fetched from DB on cache hit
    cached_data = {
        "document_hash": new_contract.document_hash,
        "risk_score": new_contract.risk_score,
        "is_cached": False,
    }
    await set_cached_analysis(doc_hash, cached_data)

    # 7. Fetch the complete contract with clauses loaded
    stmt = select(Contract).options(
        joinedload(Contract.clauses).where(Contract.id == new_contract.id)
    ).where(Contract.id == new_contract.id)
    db_result = await db.execute(stmt)
    fully_loaded = db_result.scalar_one()

    return JSONResponse(
        content={
            "document_hash": fully_loaded.document_hash,
            "risk_score": fully_loaded.risk_score,
            "filename": fully_loaded.filename,
            "clauses": fully_loaded.clauses,  # type: ignore[attr-defined]
            "is_cached": False,
        },
        status_code=201,
    )