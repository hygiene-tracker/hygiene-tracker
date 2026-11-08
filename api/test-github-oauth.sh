#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo "Missing api/.env. Copy api/.env.example to api/.env and add your GitHub OAuth credentials." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source ./.env
set +a

if [[ -z "${GITHUB_CLIENT_ID:-}" || -z "${GITHUB_CLIENT_SECRET:-}" ]]; then
  echo "Set GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET in api/.env first." >&2
  exit 1
fi

callback_uri="${GITHUB_CALLBACK_URI:-http://localhost:8080/api/auth/github/callback}"
echo "Configured callback URI: $callback_uri"
echo "After the API starts, open: http://localhost:8080/api/auth/github/login"
echo "Make sure the configured callback URI exactly matches the URL in your GitHub OAuth App."
exec sh ./gradlew bootRun
