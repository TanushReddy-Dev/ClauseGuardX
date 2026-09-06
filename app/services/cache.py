import json
from redis.asyncio import from_url

from app.core.config import settings


_redis = from_url(settings.REDIS_URL, decode_responses=True)


async def get_cached_analysis(doc_hash: str) -> dict | None:
    """Retrieve cached analysis data from Redis by document hash."""
    key = f"analysis:{doc_hash}"
    data = await _redis.get(key)
    if data is None:
        return None
    return json.loads(data)


async def set_cached_analysis(doc_hash: str, data: dict, ttl: int = 86400) -> None:
    """Cache analysis data in Redis with a TTL (default: 24 hours)."""
    key = f"analysis:{doc_hash}"
    await _redis.setex(key, ttl, json.dumps(data))