#!/usr/bin/env bash
# Builds and deploys the backend to Cloud Run.
# One-time setup (APIs, secrets, service account) is described in README.md.
set -euo pipefail

PROJECT="${PROJECT:-pill-reminder-510721}"
REGION="${REGION:-us-central1}"
SERVICE="pill-reminder-backend"

cd "$(dirname "$0")"
gcloud run deploy "$SERVICE" \
  --project "$PROJECT" --region "$REGION" --source . \
  --service-account "$SERVICE@$PROJECT.iam.gserviceaccount.com" \
  --set-secrets ANTHROPIC_API_KEY=anthropic-api-key:latest,APP_API_KEY=app-api-key:latest \
  --allow-unauthenticated \
  --timeout 300 --max-instances 2 --min-instances 0 \
  --memory 512Mi --cpu 1 --concurrency 20 \
  --quiet
