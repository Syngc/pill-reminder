"""Data shapes shared by the API and the extraction step."""

from typing import Literal

from pydantic import BaseModel, Field


class Medication(BaseModel):
    name: str = Field(description="Medicine name exactly as written on the prescription.")
    dose: str = Field(description="Amount per intake as written, e.g. '1 tableta', '5 ml', '500 mg'.")
    times: list[str] = Field(
        description="Times of day to take it, 24h 'HH:MM'. Empty if the prescription gives no frequency."
    )
    times_are_suggested: bool = Field(
        description=(
            "True when the prescription only gives a frequency (e.g. 'cada 8 horas') and the times "
            "were spread across the day as a starting point for the family to adjust."
        )
    )
    duration_days: int | None = Field(
        description="Number of days of treatment if written, otherwise null."
    )
    instructions: str = Field(
        description=(
            "Short sentence, in the user's language, repeating only what the prescription says "
            "for this medicine (e.g. 'Take with food'). Empty string if nothing extra is written."
        )
    )
    confidence: Literal["high", "low"] = Field(
        description="'low' if any part of this medicine was hard to read or ambiguous."
    )
    notes_for_reviewer: str = Field(
        description=(
            "What the family member should double-check, in the user's language. Empty string if nothing."
        )
    )


class ExtractionResult(BaseModel):
    readable: bool = Field(description="False if the image is not a legible prescription.")
    medications: list[Medication]
    warnings: list[str] = Field(
        description=(
            "General problems for the reviewer, in the user's language (blurry photo, cut-off page, etc.)."
        )
    )
