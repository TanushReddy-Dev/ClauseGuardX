import re
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession

from app.services.agent import analyze_clause
from app.schemas.clause import ClauseCreate


# ---------------------------------------------------------------------------
# Text chunking: split raw extracted text into plausible clause segments.
# Heuristic: treat double-newline paragraphs as clause boundaries; fall back
# to sentence-level splitting if the text is very short.
# ---------------------------------------------------------------------------
def _chunk_text(raw_text: str) -> list[str]:
    """Split raw contract text into clause-sized chunks.

    - Split on blank lines (two or more newlines).
    - If the result is a single chunk and the text is long enough, further
      split on sentence boundaries (periods followed by a space and uppercase).
    - Return at least one chunk (the whole text) if nothing else works.
    """
    # 1. Split on blank lines
    chunks = [c.strip() for c in re.split(r"\n\n+", raw_text) if c.strip()]

    # 2. If we only got one chunk and there's enough text, split on sentences
    if len(chunks) == 1 and len(raw_text) > 200:
        sentences = re.split(r"(?<=[.!?]) +", raw_text.strip())
        # Re‑group sentences into chunks of 2–3 sentences each
        groups: list[str] = []
        group: list[str] = []
        for s in sentences:
            group.append(s)
            if len(group) >= 3:
                groups.append(" ".join(group))
                group = []
        if group:
            groups.append(" ".join(group))
        chunks = [g for g in groups if g.strip()]

    # 3. Fallback: ensure at least one chunk
    if not chunks:
        chunks = [raw_text.strip()]

    return chunks


# ---------------------------------------------------------------------------
# Bounded analysis wrapper
# ---------------------------------------------------------------------------
async def bounded_analyze(chunk: str, index: int, semaphore) -> ClauseCreate:
    """Execute analyze_clause with a semaphore to throttle concurrency.

    Each chunk gets a unique clause_number (1‑based) attached to the result.
    """
    async with semaphore:
        result = await analyze_clause(chunk)
        result.clause_number = index + 1
        return result


# ---------------------------------------------------------------------------
# Main pipeline
# ---------------------------------------------------------------------------
async def process_contract(contract_id: UUID, raw_text: str, db: AsyncSession):
    """Process a contract's raw text: chunk, analyze in parallel, persist clauses.

    Parameters:
        contract_id: UUID of the contract in the database.
        raw_text: The full extracted text from the PDF.
        db: SQLAlchemy async session.

    Returns:
        The updated Contract object with `clauses` relationship populated.
    """
    chunks = _chunk_text(raw_text)
    if not chunks:
        # Nothing to process; return the contract as‑is
        from app.db.base import Base  # noqa: F810
        from app.models.contract import Contract  # noqa: F810
        return None  # caller should handle

    # Throttle concurrent LLM requests so we don't hit rate limits / timeouts
    semaphore = asyncio.Semaphore(10)

    # Launch all chunk analyses concurrently, limited by the semaphore
    tasks = [
        bounded_analyze(chunk, i, semaphore)
        for i, chunk in enumerate(chunks)
    ]
    results = await asyncio.gather(*tasks)

    # Map ClauseCreate → SQLAlchemy Clause model instances
    # (Import here to avoid circular imports at module level)
    from app.db.base import Base  # noqa: F810
    from app.models.contract import Clause  # noqa: F810

    clause_models = []
    for clause_create in results:
        clause_models.append(
            Clause(
                contract_id=contract_id,
                category=clause_create.category,
                original_text=clause_create.original_text,
                risk_level=clause_create.risk_level,
                explanation=clause_create.explanation,
                negotiation_strategy=clause_create.negotiation_strategy,
                clause_number=clause_create.clause_number,
            )
        )

    # Persist transactionally
    db.add_all(clause_models)
    await db.commit()
    await db.flush()

    return clause_models