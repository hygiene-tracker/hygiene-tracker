# Hygiene Tracker

Hygiene Tracker helps people who want to improve their hygiene build a showering habit. Users log
each shower (duration and products used), keep a daily streak and a score that rewards showering
and penalizes skipped days, and compete on a leaderboard of 30-day scores. Sign-in is with GitHub
or Google. Admins moderate the leaderboard.

## Contents

- [Live URLs](#live-urls)
- [Tech stack](#tech-stack)
- [Repository layout](#repository-layout)
- [Prerequisites](#prerequisites)
  - [Commands on each OS](#commands-on-each-os)
- [Run it locally](#run-it-locally)
  - [1. Start the API and database](#1-start-the-api-and-database)
  - [2. Start the web app](#2-start-the-web-app)
  - [3. Android (optional for a local run)](#3-android-optional-for-a-local-run)
  - [4. Sign-in (optional for a local run)](#4-sign-in-optional-for-a-local-run)
  - [5. Stop the stack](#5-stop-the-stack)
- [Run the tests](#run-the-tests)
  - [API tests](#api-tests)
  - [App lint](#app-lint)
  - [API connection check](#api-connection-check)
  - [Sign-in check (team only)](#sign-in-check-team-only)
  - [CI](#ci)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [Team](#team)

---

## Live URLs

Render's free tier puts the API to sleep after 15 idle minutes, so the first request can take
a few minutes.

| What | URL |
|---|---|
| Web app | <https://hygiene-tracker-web.onrender.com> |
| API | <https://hygiene-tracker.onrender.com> |
| API health | <https://hygiene-tracker.onrender.com/actuator/health> |
| API docs (Swagger UI) | <https://hygiene-tracker.onrender.com/swagger-ui.html> |

## Tech stack

| Part | Technology |
|---|---|
| API | Java 21, Spring Boot 4, Gradle, Spring Security (OAuth2 Resource Server), Flyway |
| Database | PostgreSQL 18 (Docker locally, Neon in production) |
| App | React Native with Expo, TypeScript (web and Android) |
| Sign-in | GitHub and Google (Authorization Code + PKCE); the API issues its own JWT |
| Tests | JUnit 5, Testcontainers (real PostgreSQL), JaCoCo (70% on the service layer) |
| CI | GitHub Actions on every pull request |

## Repository layout

```text
api/          Spring Boot API (Gradle project, Dockerfile)
app/          Expo app (app/hygiene-tracker-react)
docs/         PRD, ERD, error model, OpenAPI contract, team docs
compose.yml   Local stack: API + PostgreSQL
.env.example  Local configuration to copy to .env
```

---

## Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| [Docker Desktop](https://docs.docker.com/get-docker/) (or Docker Engine with Compose) | Compose v2 or newer | Running the stack, and the API tests (Testcontainers) |
| JDK | 21 | Running the API tests |
| Node.js | 20 or newer | The Expo app |
| Android Studio | Latest, with Android SDK Platform 36 | The Android app (optional for a local run) |

### Commands on each OS

The commands in this README are for a Bash shell: the terminal on macOS and Linux, or **Git Bash**
or **WSL** on Windows. Start Docker Desktop before running them (on Linux, Docker Engine with the
Compose plugin; add your user to the `docker` group or use `sudo`).

In Windows **PowerShell**, these commands differ:

| Bash | PowerShell |
|---|---|
| `cp .env.example .env` | `Copy-Item .env.example .env` |
| `./gradlew build` | `.\gradlew.bat build` |
| `API_BASE_URL=https://... npm run test:connection` | `$env:API_BASE_URL="https://..."; npm run test:connection` |

---

## Run it locally

### 1. Start the API and database

From the repository root:

```bash
cp .env.example .env
docker compose up --build
```

The first build downloads the base images and Gradle dependencies, so it takes a few minutes.
When the API logs `Started ApiApplication`, check it:

| What | URL | Expected |
|---|---|---|
| Health | <http://localhost:8080/actuator/health> | `{"status":"UP", ...}` |
| API docs (Swagger UI) | <http://localhost:8080/swagger-ui.html> | The API's endpoints |

Flyway creates the database tables on the first start. The database keeps its data between runs
in a Docker volume.

If `docker compose up` fails with "Set POSTGRES_PASSWORD in .env", the `cp .env.example .env`
step was skipped.

### 2. Start the web app

Leave the stack running and open a second terminal:

```bash
cd app/hygiene-tracker-react
cp .env.example .env
npm install
npx expo start --web
```

The web app opens at <http://localhost:8081> and talks to the API at `http://localhost:8080`.

### 3. Android (optional for a local run)

Android needs Android Studio and a development build (Expo Go cannot register the app's sign-in
redirect). If Expo reports "Failed to resolve the Android SDK path", set `ANDROID_HOME` to your
SDK folder (Android Studio, Settings, Android SDK shows the path). With an emulator running or a
phone connected over USB, from `app/hygiene-tracker-react`:

```bash
npx expo run:android
```

The first build takes several minutes; after that, `npx expo start` and press `a`.

On Android, `localhost` is the device itself, so change `EXPO_PUBLIC_API_BASE_URL` in the app's
`.env` first: `http://10.0.2.2:8080` for the emulator, or `http://<your computer's LAN IP>:8080`
for a physical phone.

### 4. Sign-in (optional for a local run)

You don't need sign-in to run the stack or the tests. Without OAuth credentials, the sign-in
endpoints return `503` and everything else works. To try sign-in, use the
[live web app](https://hygiene-tracker-web.onrender.com).

The OAuth client secrets are private to the team and are never shared. Team members set up local
sign-in with these values (secrets go only in the API's environment):

- **API** (root `.env`): `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GOOGLE_CLIENT_IDS`,
  `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_WEB_CLIENT_SECRET`.
- **App** (`app/hygiene-tracker-react/.env`): the public `EXPO_PUBLIC_*` client IDs.

See the [app README](app/hygiene-tracker-react/README.md) for the redirect URIs to register.

### 5. Stop the stack

Press `Ctrl+C` in each terminal, then from the repository root run `docker compose down`. To also
delete the local database: `docker compose down -v`.

---

## Run the tests

### API tests

Docker must be running: the tests start a throwaway PostgreSQL 18 with Testcontainers.

```bash
cd api
./gradlew build
```

This compiles, runs every test, writes the coverage report to
`api/build/reports/jacoco/test/html/index.html`, and fails if the service layer is below 70%
coverage.

### App lint

```bash
cd app/hygiene-tracker-react
npm run lint
```

### API connection check

With the API running (step 1), from `app/hygiene-tracker-react`:

```bash
npm run test:connection
```

This checks the API health endpoint, CORS preflight for `http://localhost:8081`, and that the
Google and GitHub sign-in routes respond (`503` without OAuth credentials is expected). It sends
invalid sign-in values on purpose, so it does not create an account or need a real login.
`npm run test:api` runs the same check.

To check the hosted API instead, set `API_BASE_URL` (and `FRONTEND_ORIGIN` if your web app uses a
different origin). If Render is asleep, the first check waits up to 3 minutes for it to wake up:

```bash
API_BASE_URL=https://hygiene-tracker.onrender.com \
FRONTEND_ORIGIN=http://localhost:8081 \
npm run test:connection
```

### Sign-in check (team only)

Team members with local OAuth credentials ([step 4](#4-sign-in-optional-for-a-local-run)) can check the full
sign-in flow: start the web app, open the login screen, sign in with Google and GitHub one at a
time, confirm the Explore screen shows the signed-in account, reload the page to check the session
persists, then use **Sign out** and check the login screen returns.

### CI

Every pull request runs the API tests, builds the Docker image and runs a gitleaks secret scan in
GitHub Actions, and CodeQL scans the code. A pull request can't merge until its required checks
pass. Dependabot opens weekly pull requests for npm and Gradle updates.

---

## Configuration

Local values live in `.env` (copied from [`.env.example`](.env.example)). `.env` is ignored by
Git; never commit it. For production operations (deploy, roll back, secrets, logs), see the
[runbook](docs/runbook.md).

| Variable | Used by | Local value |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | The local database | From `.env.example` |
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | The API (`compose.yml` sets them to the `db` service) | From `.env.example` |
| `JWT_SECRET` | Signing the API's tokens; at least 32 characters or the API won't start | Placeholder in `.env.example` |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GOOGLE_CLIENT_IDS`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_WEB_CLIENT_SECRET` | Sign-in | Empty (sign-in returns 503) |
| `CORS_ALLOWED_ORIGINS` | Web origins allowed to call the API (comma-separated) | `http://localhost:8081,https://hygiene-tracker-web.onrender.com` |
| `PORT` | The port the API listens on | Not set (defaults to `8080`) |

## Deployment

| Part | Where | Notes |
|---|---|---|
| API | Render web service | Built from `api/Dockerfile`; health check `/actuator/health` |
| Web app | Render static site | `npx expo export --platform web` from `app/hygiene-tracker-react`, publishing `dist` |
| Database | Neon (PostgreSQL 18) | Flyway creates and updates the tables when the API starts |

All production values, including secrets, are set only in Render's environment settings. Set
`CORS_ALLOWED_ORIGINS` on the API service to
`http://localhost:8081,https://hygiene-tracker-web.onrender.com` while testing the local web app;
then redeploy or restart the API service so Spring Boot reloads the value.

---

## Documentation

| Document | What it covers |
|---|---|
| [PRD](docs/prd.md) | Users, problem, success measures, scope, user stories and acceptance criteria |
| [ERD](docs/erd.md) | Tables, keys, relationships and what happens on delete |
| [OpenAPI contract](docs/openapi.yaml) | Every endpoint; lint with `npx @redocly/cli lint docs/openapi.yaml` |
| [Error model](docs/error-model.md) | The Problem Details error shape and status codes |
| [Runbook](docs/runbook.md) | Deploy, roll back, rotate a secret, find logs, database unreachable |
| [Threat model](docs/security.md) | Five threats and the tests, config and workflows that address them |
| [Team agreement](docs/team_agreement.md) | PR deadline, review turnaround, team channel |
| [Merge captains](docs/captains.md) | Who merges each sprint |
| [ADRs](docs/adr/) | Architecture decisions: database, sign-in, deployment |

---

## Contributing

- Work on a branch, never on `main`. Every change reaches `main` through a pull request that
  references an issue (`Closes #N`).
- A pull request needs passing CI and an approval from a teammate who isn't the author, and only
  that sprint's merge captain or deputy merges it.
- Update `docs/openapi.yaml` in the same pull request when an endpoint changes.

## Team

| Name | GitHub |
|---|---|
| Jaime Serrano Acevedo | [@jai-serr-ace](https://github.com/jai-serr-ace) |
| Leonardo Lopez | [@leodebuss](https://github.com/leodebuss) |
| Hugo Ruiz-Mireles | [@Hugo-RM](https://github.com/Hugo-RM) |
