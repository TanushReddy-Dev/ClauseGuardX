from tenacity import retry, stop_after_attempt, wait_exponential

from app.core.llm import structured_client
from app.schemas.clause import ClauseCreate


# ---------------------------------------------------------------------------
# Retry configuration (Tenacity)
# ---------------------------------------------------------------------------
# 5 attempts total with exponential backoff:
#   attempt 1: immediate
#   attempt 2: +2s
#   attempt 3: +4s
#   attempt 4: +8s (capped at 10s)
#   attempt 5: +10s
# Any transient failure — 429 rate limit, timeout, or 5xx — will trigger a
# retry before the router cascades to the next model in the LLM layer.
analyze_clause_retry = retry(
    stop=stop_after_attempt(5),
    wait=wait_exponential(multiplier=1, min=2, max=10),
)


async def analyze_clause(chunk_text: str) -> ClauseCreate:
    """Analyze a contract text chunk and return a structured ClauseCreate schema.

    The Tenacity retry decorator is applied at the function level so that
    transient failures (rate limits, timeouts) are retried exponentially before
    the LiteLLM router automatically fails over to the next model in the
    cascade (groq → gemini → openai).

    Returns:
        ClauseCreate — a Pydantic v2 model validated by Instructor against
        the LLM's JSON output.
    """
    @analyze_clause_retry
    async def _analyze() -> ClauseCreate:
        return await structured_client.chat.completions.create(
            # The router uses the model_name defined in app.core.llm.model_list.
            # The first available model in the cascade will be selected automatically.
            model="groq/llama3-8b-8192",
            response_model=ClauseCreate,
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are a contract risk analyst. Given a raw clause excerpt, "
                        "classify the risk level into exactly one of: LOW, MEDIUM, or HIGH. "
                        "Provide a concise explanation (1–2 sentences) justifying the rating. "
                        "Generate a 3-bullet negotiation strategy for stakeholders to mitigate "
                        "the risk.  Output MUST be valid JSON matching the ClauseCreate schema: "
                        "{category, original_text, risk_level, explanation, negotiation_strategy, "
                        "clause_number?}.  Do NOT include any reasoning, apologies, or extra text. "
                        "If the text is unclear or insufficient, set risk_level to MEDIUM and "
                        "explain why."
                    ),
                },
                {
                    "role": "user",
                    "content": chunk_text,
                },
            ],
        )

    return await _analyze()