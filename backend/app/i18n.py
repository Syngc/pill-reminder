"""User-facing text in the languages the app supports."""

from typing import Literal

Language = Literal["es", "en"]
DEFAULT_LANGUAGE: Language = "es"

LANGUAGE_NAMES: dict[Language, str] = {"es": "Spanish", "en": "English"}

MESSAGES: dict[str, dict[Language, str]] = {
    "invalid_api_key": {
        "es": "Clave de acceso inválida.",
        "en": "Invalid access key.",
    },
    "unsupported_type": {
        "es": "La imagen debe ser JPEG, PNG, WEBP o GIF.",
        "en": "The image must be JPEG, PNG, WEBP or GIF.",
    },
    "empty_image": {
        "es": "La imagen está vacía.",
        "en": "The image is empty.",
    },
    "image_too_large": {
        "es": "La imagen supera 5 MB.",
        "en": "The image is larger than 5 MB.",
    },
    "unprocessable": {
        "es": "No se pudo procesar la imagen.",
        "en": "The image could not be processed.",
    },
    "busy": {
        "es": "Servicio ocupado, intenta de nuevo en un momento.",
        "en": "The service is busy. Please try again in a moment.",
    },
    "service_error": {
        "es": "Error del servicio de lectura.",
        "en": "The reading service had an error.",
    },
    "no_connection": {
        "es": "No hay conexión con el servicio de lectura.",
        "en": "Could not reach the reading service.",
    },
    "refused": {
        "es": "No se pudo leer esta receta. Intenta con otra foto.",
        "en": "This prescription could not be read. Try another photo.",
    },
    "too_long": {
        "es": "La receta es demasiado larga para procesarla de una vez.",
        "en": "The prescription is too long to process at once.",
    },
    "invalid_response": {
        "es": "Respuesta inválida del servicio de lectura.",
        "en": "The reading service returned an invalid response.",
    },
}


def message(key: str, language: Language) -> str:
    return MESSAGES[key][language]


def language_from_header(accept_language: str | None) -> Language:
    """Picks the app language from an Accept-Language header such as 'en-US,en;q=0.9'."""
    if not accept_language:
        return DEFAULT_LANGUAGE
    first = accept_language.split(",")[0].split(";")[0].strip().lower()
    primary = first.split("-")[0]
    return primary if primary in LANGUAGE_NAMES else DEFAULT_LANGUAGE
