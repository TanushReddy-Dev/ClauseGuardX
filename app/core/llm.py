import instructor
from litellm import Router
from tenacity import retry, stop_after_attempt, wait_exponential

from app.core.config import settings


# ---------------------------------------------------------------------------
# Model List with Fallback Cascade
# ---------------------------------------------------------------------------
# Priority order:
#   1. groq/llama3-8b-8192   — fast, low cost (primary)
#   2. gemini/gemini-2.5-pro — reasoning / complex analysis (fallback)
#   3. openai/gpt-4o-mini    — safe default (last resort)
model_list = [
    {
        "model_name": "groq/llama3-8b-8192",
        "litellm_params": {
            "temperature": 0.0,
        },
        "litellm_family": "groq",
    },
    {
        "model_name": "gemini/gemini-2.5-pro",
        "litellm_params": {
            "temperature": 0.0,
        },
        "litellm_family": "gemini",
    },
    {
        "model_name": "openai/gpt-4o-mini",
        "litellm_params": {
            "temperature": 0.0,
        },
        "litellm_family": "openai",
    },
]


# ---------------------------------------------------------------------------
# Router — dynamic failover across providers
# ---------------------------------------------------------------------------
router = Router(model_list=model_list)


# ---------------------------------------------------------------------------
# Instructor‑wrapped async client
# ---------------------------------------------------------------------------
# We wrap the router's async completion so that every call is automatically
# retried (Tenacity) and returns structured Pydantic objects (Instructor).
# The `acompletion` method is used so the whole stack is async.
litellm_router = Router(model_list=model_list)
structured_client = instructor.from_litellm(
    litellm_router.acompletion,
    # mode tells Instructor how to parse the response.  MODE_JSON means we
    # expect a JSON‑serialisable dict that will be coerced into the target
    # Pydantic model.
    mode=instructor.Mode.JSON,
)

# ---------------------------------------------------------------------------
# Retry wrapper (Tenacity)
# ---------------------------------------------------------------------------
# Retry up to 4 times with exponential back‑off (1s, 2s, 4s).  Any
# transient failure — rate limit, timeout, or 5xx — will trigger a retry
# before the router moves to the next model in the cascade.
llm_retry = retry(
    stop=stop_after_attempt(4),
    wait=wait_exponential(multiplier=1, min=1, max=10),
)


async def complete_with_fallback(
    *,
    model: str,
    messages: list[dict[str, str]],
    response_model: type,
    **kwargs,
) -> type:
    """Execute a structured LLM call with full retry + fallback support.

    The function tries the requested *model*.  If it fails after 4 attempts,
    the router automatically cascades to the next model in the list.
    The caller does not need to handle fallovers manually.
    """
    @llm_retry
    async def _call() -> type:
        return await structured_client.acompletion(
            model=model,
            messages=messages,
            response_model=response_model,
            **kwargs,
        )

    return await _call()


# Export the ready‑to‑use client
__all__ = ["structured_client", "complete_with_fallback", "llm_retry"]