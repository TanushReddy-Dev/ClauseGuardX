import asyncio
from unittest.mock import patch

import httpx
import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture(scope="session")
def client():
    """Provide an httpx.AsyncClient connected to the FastAPI app."""
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture(scope="session")
def pdf_bytes():
    """Provide a small dummy PDF byte stream for testing."""
    # A minimal valid PDF: %PDF-1.4 with one empty page
    return b"""%PDF-1.4
%âãÏÓ 0 0 0 0 R
1 0 obj
/Type /Catalog /Pages 2 0 obj
endobj
2 0 obj
/Type /Pages /Kids [3 0 obj] /Count 1
endobj
3 0 obj
/Type /Page /Parent 2 0 /MediaBox [0 0 612 792] /Resources 4 0 obj
endobj
4 0 obj
/Type /XObject /Subtype /Image /Width 1 /Height 1 /Length 4
stream
im0B
endstream
endobj
xref
0 5
0000000000 65535 f
0000000009 0000000000 n
0000000040 0000000000 n
0000000075 0000000000 n
0000000108 0000000000 n
trailer
/Size 5
root object
startxref
183
%%EOF"""


@pytest.fixture(scope="function")
def mock_analyze_clause():
    """Mock the analyze_clause function in app.services.agent."""
    from app.services.agent import analyze_clause
    from app.schemas.clause import ClauseCreate

    static_clause = ClauseCreate(
        category="Test",
        original_text="dummy text",
        risk_level="LOW",
        explanation="Test clause",
        negotiation_strategy="Test strategy",
    )

    with patch("app.services.agent.analyze_clause", return_value=static_clause):
        yield static_clause