from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/clauseguard"
    REDIS_URL: str = "redis://localhost:6379/0"
    APP_ENV: str = "development"

    class Config:
        env_file = ".env"


settings = Settings()