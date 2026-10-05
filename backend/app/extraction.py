"""Turns a prescription photo into structured medications using Claude vision."""

import base64
import logging

import anthropic
from pydantic import ValidationError

from .i18n import DEFAULT_LANGUAGE, LANGUAGE_NAMES, Language
from .schemas import ExtractionResult

log = logging.getLogger(__name__)

MODEL = "claude-opus-5-5"

SYSTEM_PROMPT = """\
You transcribe medical prescriptions for an app that reminds older adults to take their \
medicines. A family member photographs the prescription; your output is shown to them for \
review before anything is scheduled.

Your job is transcription, not medical judgement:
- Copy medicine names and doses exactly as written. Never add, correct, or substitute a \
medicine, dose, interaction warning or advice that is not on the paper.
- The name is what will be read aloud at each dose, so keep the medicine's name, form and \
strength ("Acetaminofén 500 mg tabletas", "Acebrofilina jarabe 50 mg/5 ml") and leave out \
package and dispensing details: bottle or box size and unit counts ("fco x 120 ml", \
"caj x 16", "#30"). Expand abbreviations only when unambiguous (tab → tabletas).
- If a value is illegible or ambiguous, give your best reading, set confidence to "low" and \
explain in notes_for_reviewer what to verify. Do not guess silently.
- Times: if the prescription names specific moments (morning, noon, afternoon, night; in \
Spanish mañana, mediodía, tarde, noche), map them to 24h HH:MM (08:00, 13:00, 18:00, 21:00). \
Set times_are_suggested to false only when exact clock times were written. If it only gives \
a frequency ("every 8 hours", "cada 8 horas", "3 times a day"), spread the doses evenly \
starting at 08:00 and set times_are_suggested to true. If no frequency is given, leave times \
empty.
- As-needed medicines (PRN, "según necesidad"): include them with empty times and say so in \
the notes.
- If the image is not a prescription or is unreadable, set readable to false, return no \
medications and explain in warnings.

Language: write instructions, notes_for_reviewer and warnings in {language}, in plain, short \
sentences. If the prescription is written in another language, translate its instructions \
faithfully: do not add, drop or soften anything. Medicine names and doses stay as written.
"""

USER_TEXT: dict[Language, str] = {
    "es": "Transcribe esta receta.",
    "en": "Transcribe this prescription.",
}


class ExtractionError(Exception):
    """Raised when extraction fails in a way the client should see. `key` indexes i18n.MESSAGES."""

    def __init__(self, key: str, status_code: int = 502):
        super().__init__(key)
        self.key = key
        self.status_code = status_code


def _strict_schema() -> dict:
    """Pydantic's JSON schema plus additionalProperties: false, as structured outputs require."""

    def close(node):
        if isinstance(node, dict):
            if node.get("type") == "object":
                node["additionalProperties"] = False
            for value in node.values():
                close(value)
        elif isinstance(node, list):
            for item in node:
                close(item)
        return node

    return close(ExtractionResult.model_json_schema())


_SCHEMA = _strict_schema()


def extract_prescription(
    client: anthropic.Anthropic, image_bytes: bytes, media_type: str, language: Language = DEFAULT_LANGUAGE
) -> ExtractionResult:
    image_b64 = base64.standard_b64encode(image_bytes).decode("utf-8")

    try:
        response = client.beta.messages.create(
            model=MODEL,
            max_tokens=16000,
            system=SYSTEM_PROMPT.format(language=LANGUAGE_NAMES[language]),
            output_config={
                "effort": "medium",
                "format": {"type": "json_schema", "schema": _SCHEMA},
            },
            betas=["server-side-fallback-2026-07-01"],
            fallbacks="default",
            messages=[
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image",
                            "source": {"type": "base64", "media_type": media_type, "data": image_b64},
                        },
                        {"type": "text", "text": USER_TEXT[language]},
                    ],
                }
            ],
        )
    except anthropic.BadRequestError as e:
        log.warning("Claude rejected the request: %s", e.message)
        raise ExtractionError("unprocessable", status_code=422) from e
    except anthropic.RateLimitError as e:
        raise ExtractionError("busy", status_code=503) from e
    except anthropic.APIStatusError as e:
        log.error("Claude API error %s: %s", e.status_code, e.message)
        raise ExtractionError("service_error") from e
    except anthropic.APIConnectionError as e:
        raise ExtractionError("no_connection") from e

    if response.stop_reason == "refusal":
        log.warning("Extraction refused: %s", response.stop_details)
        raise ExtractionError("refused", status_code=422)
    if response.stop_reason == "max_tokens":
        raise ExtractionError("too_long", status_code=422)

    text = next((b.text for b in response.content if b.type == "text"), None)
    if text is None:
        raise ExtractionError("invalid_response")
    try:
        return ExtractionResult.model_validate_json(text)
    except ValidationError as e:
        log.error("Invalid extraction JSON (request %s): %s", response._request_id, e)
        raise ExtractionError("invalid_response") from e
