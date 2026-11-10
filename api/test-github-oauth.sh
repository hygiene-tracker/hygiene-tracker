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

echo "GitHub PKCE exchange is configured on the API."
echo "Set EXPO_PUBLIC_GITHUB_CLIENT_ID in app/hygiene-tracker-react/.env.example's copied .env."
echo "Start the app and use the Continue with GitHub button to exercise the PKCE flow."
exec sh ./gradlew bootRun
