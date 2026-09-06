# ClauseGuard Core

A headless, multi-platform AI pipeline and Android presentation layer for contract risk analysis.

ClauseGuard ingests raw PDF contracts, extracts the text, hashes it for deduplication, and runs a concurrent LLM pipeline to classify individual clauses by risk level (LOW, MEDIUM, HIGH) while providing plain-English explanations and negotiation strategies.

## Architecture

The project is built on Clean Architecture principles and spans a Python backend and a native Kotlin Android client.

### Backend (FastAPI)
- **Framework:** FastAPI
- **Database:** PostgreSQL (Asyncpg) + SQLAlchemy 2.0 (Async) + Alembic
- **Caching:** Redis
- **AI Orchestration:** 
  - **LiteLLM Router:** Handles model failover (Groq → Gemini → OpenAI) to bypass rate limits.
  - **Instructor:** Guarantees strict JSON output conforming to Pydantic v2 schemas.
  - **Tenacity:** Handles transient network failures with exponential backoff.
- **Concurrency:** `asyncio.Semaphore` throttles parallel clause analysis, dropping processing time from ~40s to ~3s.

### Android Client (Kotlin)
- **Domain Layer:** Pure Kotlin use cases and models (`Contract`, `Clause`, `RiskLevel`).
- **Data Layer:** 
  - **Network:** Ktor Client with JSON content negotiation.
  - **Persistence:** SQLDelight for local SQLite storage (transactional batch inserts).
  - **OCR:** Headless Google ML Kit integration (`OcrProcessor.kt`) decoupled from the UI.
- **Presentation Layer (MVI):** 
  - `ContractUiState` (StateFlow), `ContractIntent`, and `ContractSideEffect` (SharedFlow).
  - Jetpack Compose UI featuring hardware-accelerated, interruptible spring animations (Apple-style design philosophy) and a Modern Builder SaaS aesthetic.

---

## Getting Started

### 1. Start the Backend Infrastructure

You need Docker installed to run the database and cache.

```bash
# Start Postgres and Redis
docker-compose up -d postgres redis

# Set up the Python environment
python -m venv venv
source venv/Scripts/activate  # Or `venv\Scripts\activate` on Windows
pip install -r requirements.txt

# Run database migrations
alembic upgrade head
```

### 2. Configure Environment Variables

Create a `.env` file in the root directory:

```env
DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/clauseguard
REDIS_URL=redis://localhost:6379/0
APP_ENV=development

# LLM API Keys (LiteLLM requires at least one of these based on your configuration)
GROQ_API_KEY=gsk_your_key_here
# GEMINI_API_KEY=your_key_here
# OPENAI_API_KEY=sk-your_key_here
```

### 3. Run the FastAPI Server

```bash
uvicorn app.main:app --reload --port 8000
```
The API will be available at `http://localhost:8000/api/v1`.

### 4. Run the Android Client

1. Open the `android/` directory in **Android Studio**.
2. Add a sample PDF named `dummy_contract.pdf` to `android/src/main/assets/`.
3. Launch an Android Emulator.
   *(Note: The Ktor client is configured to connect to `http://10.0.2.2:8000`, the standard emulator alias for your host machine's localhost).*
4. Run the app. 
5. Open **Logcat** and filter by `ClauseGuard-Core` to see the headless pipeline execute, or wire `MainActivity.kt` to the `AnalysisResultsScreen` composable to view the UI.

---

## Testing

### Backend (pytest)
The Python backend includes End-to-End (E2E) tests that mock the LLM calls to avoid API costs.

```bash
pytest tests/
```

### Android (JUnit + Robolectric)
The Kotlin data and domain layers are tested using an in-memory SQLDelight driver and a Ktor MockEngine.

```bash
cd android
./gradlew testDebugUnitTest
```

---

## Design Philosophy

The Android UI (`AnalysisResultsScreen.kt`) was built using the following design engineering principles:
- **Visuals:** "Modern Builder SaaS" aesthetic (warm dark background `#08090A`, hairline borders, single accent color).
- **Motion:** Hardware-accelerated entrances, staggered delays, and interruptible spring-based expansions (`dampingRatio = NoBouncy`) rather than rigid CSS keyframes.
- **Feedback:** Immediate `.scale(0.97f)` transforms on tap targets to eliminate perceived latency.