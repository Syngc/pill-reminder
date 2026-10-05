# Backend

A small FastAPI service that turns a prescription photo into structured medicines using Claude vision (`claude-opus-5-5`).

## Run

```bash
cp .env.example ../.env  # set ANTHROPIC_API_KEY (and optionally APP_API_KEY)
uv sync
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Settings are read from the repo-root `.env`, then `backend/.env` (which overrides it). Both are git-ignored. The Anthropic key is held as a secret value, so it never appears in logs or error messages.

Tests (no network; the Claude client is faked):

```bash
uv run pytest
```

## API

`POST /v1/prescriptions/extract`: multipart form with an `image` field (JPEG/PNG/WEBP/GIF, max 5 MB). If `APP_API_KEY` is set, send it in the `X-API-Key` header.

Send `Accept-Language: es` or `Accept-Language: en` (Spanish is the default; any other language falls back to Spanish). It controls the language of `instructions`, `notes_for_reviewer`, `warnings` and every error message. Medicine names and doses are always copied as written. If the prescription is in a different language from the one requested, its instructions are translated faithfully, and the family member checks them during review.

```json
{
  "readable": true,
  "medications": [
    {
      "name": "Losartán",
      "dose": "50 mg",
      "times": ["08:00", "20:00"],
      "times_are_suggested": true,
      "duration_days": 30,
      "instructions": "Tomar con agua",
      "confidence": "high",
      "notes_for_reviewer": ""
    }
  ],
  "warnings": []
}
```

Errors return `{"detail": "<message in the requested language>"}`. The app shows the message as-is. All user-facing text lives in [`app/i18n.py`](app/i18n.py).

`GET /health` returns `{"status": "ok"}`.

## How extraction works

- [`app/extraction.py`](app/extraction.py) sends the image with a system prompt that limits the model to **transcription only**: no added medicines, doses or advice. Anything unclear is marked `confidence: "low"` with a note for the reviewer.
- Structured outputs (`output_config.format` with a JSON schema built from the Pydantic models in [`app/schemas.py`](app/schemas.py)) guarantee the response shape.
- If the prescription only gives a frequency ("cada 8 horas"), times are spread from 08:00 and flagged `times_are_suggested`, so the family adjusts them during review.
- Server-side refusal fallbacks (`fallbacks: "default"`) are enabled. A request that is still refused returns a 422 asking for another photo.

## Status

All code paths are covered by unit tests with a fake client. The service has **not yet made a live call** to the Claude API. Run one real prescription through it once a key is configured.
