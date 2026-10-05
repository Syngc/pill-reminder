# MedRing

An Android app that helps older adults take their medicines on time, built for the ML Empowerment Build Challenge.

1. A family member photographs the prescription.
2. A vision model (Claude) reads each medicine, dose and schedule.
3. The family member reviews and corrects everything, then confirms.
4. At each dose time the phone rings like an alarm clock, even when locked, and reads the instructions aloud.
5. The person taps one large button, **YA ME LA TOMÉ** / **I TOOK IT**, to confirm.

The app is fully available in **Spanish and English**. The person picks the language inside the app, and screens, spoken alarms, notifications and the AI's instructions and notes all follow that choice.

The app only repeats what the prescription says. It never gives medical advice.

## Repository layout

| Folder | What | Stack |
| --- | --- | --- |
| [`backend/`](backend/) | Prescription extraction API | Python, FastAPI, Anthropic SDK |
| [`mobile/`](mobile/) | Android app | Kotlin, Jetpack Compose, Room |
| [`website/`](website/) | Project website (not started) | — |

## Quick start

```bash
# 1. Backend
cd backend
cp .env.example ../.env         # add ANTHROPIC_API_KEY (repo-root .env)
uv sync
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000

# 2. App (in another terminal; needs JDK 17+ and the Android SDK)
cd mobile
./gradlew installDebug          # emulator reaches the backend at http://10.0.2.2:8000
```

See each folder's README for details.
