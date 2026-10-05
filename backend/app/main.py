import logging
import secrets
from functools import lru_cache
from pathlib import Path

import anthropic
from fastapi import Depends, FastAPI, Header, HTTPException, UploadFile
from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict
from starlette.concurrency import run_in_threadpool

from .extraction import ExtractionError, extract_prescription
from .i18n import Language, language_from_header, message
from .schemas import ExtractionResult

logging.basicConfig(level=logging.INFO)

ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp", "image/gif"}
MAX_IMAGE_BYTES = 5 * 1024 * 1024  # Claude's per-image limit for base64 input


BACKEND_DIR = Path(__file__).resolve().parents[1]


class Settings(BaseSettings):
    # The repo-root .env is shared; backend/.env, if present, overrides it.
    model_config = SettingsConfigDict(
        env_file=(BACKEND_DIR.parent / ".env", BACKEND_DIR / ".env"),
        extra="ignore",
    )
    app_api_key: str = ""
    # SecretStr keeps the key out of reprs, logs and tracebacks.
    anthropic_api_key: SecretStr | None = None


@lru_cache
def get_settings() -> Settings:
    return Settings()


@lru_cache
def get_client() -> anthropic.Anthropic:
    key = get_settings().anthropic_api_key
    # Without a key in .env, the SDK falls back to the environment or an `ant auth login` profile.
    return anthropic.Anthropic(api_key=key.get_secret_value() if key else None)


def get_language(accept_language: str | None = Header(default=None)) -> Language:
    return language_from_header(accept_language)


def require_api_key(
    x_api_key: str | None = Header(default=None),
    settings: Settings = Depends(get_settings),
    language: Language = Depends(get_language),
) -> None:
    if settings.app_api_key and not secrets.compare_digest(x_api_key or "", settings.app_api_key):
        raise HTTPException(status_code=401, detail=message("invalid_api_key", language))


app = FastAPI(title="Pill Reminder Backend", version="0.1.0")


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post(
    "/v1/prescriptions/extract",
    response_model=ExtractionResult,
    dependencies=[Depends(require_api_key)],
)
async def extract(
    image: UploadFile,
    client: anthropic.Anthropic = Depends(get_client),
    language: Language = Depends(get_language),
) -> ExtractionResult:
    if image.content_type not in ALLOWED_TYPES:
        raise HTTPException(status_code=415, detail=message("unsupported_type", language))
    data = await image.read()
    if not data:
        raise HTTPException(status_code=400, detail=message("empty_image", language))
    if len(data) > MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail=message("image_too_large", language))

    try:
        # The SDK call is blocking; run it off the event loop.
        return await run_in_threadpool(extract_prescription, client, data, image.content_type, language)
    except ExtractionError as e:
        raise HTTPException(status_code=e.status_code, detail=message(e.key, language)) from e
