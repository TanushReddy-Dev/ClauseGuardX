import hashlib

import fitz


def extract_text_from_pdf(file_bytes: bytes) -> str:
    """Extract clean text from PDF bytes using pymupdf.

    Opens the PDF from a byte stream, iterates all pages, and concatenates
    the text content.  Whitespace / newlines are normalised by the caller
    (compute_sha256).
    """
    doc = fitz.open(stream=file_bytes, filetype="pdf")
    texts = [page.get_text("text") for page in doc]
    doc.close()
    return "".join(texts)


def compute_sha256(text: str) -> str:
    """Normalize whitespace/newlines, encode UTF-8, return SHA-256 hex digest."""
    normalized = " ".join(text.split())
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()