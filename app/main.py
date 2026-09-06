from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.endpoints.contracts import router as contracts_router

app = FastAPI(
    title="ClauseGuard Core",
    version="2.0.0",
)

# CORS — allow all origins for headless / local development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


app.include_router(contracts_router, prefix="/api/v1")