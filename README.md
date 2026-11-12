# Hygiene Tracker

Hygiene Tracker helps people who want to improve their hygiene build a showering habit. Users log
each shower (duration and products used), keep a daily streak and a score that rewards showering
and penalizes skipped days, and compete on a leaderboard of 30-day scores. Sign-in is with GitHub
or Google. Admins moderate the leaderboard.

**Live URLs** (Render free tier: the API sleeps after 15 idle minutes, so the first request can
take about a minute):

| What | URL |
|---|---|
| Web app | <https://hygiene-tracker-web.onrender.com> |
| API | <https://hygiene-tracker.onrender.com> |
| API health | <https://hygiene-tracker.onrender.com/actuator/health> |
| API docs (Swagger UI) | <https://hygiene-tracker.onrender.com/swagger-ui.html> |

---

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
| JDK | 21 | Running the API tests or the API outside Docker |
| Node.js | 20 or newer | The Expo app |
| Android Studio | Latest, with Android SDK Platform 36 | The Android app (development build) |

On Windows, use the same commands from WSL or Git Bash, or replace `./gradlew` with
`gradlew.bat`.

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

To stop: `Ctrl+C`, then `docker compose down`. To also delete the local database:
`docker compose down -v`.

If `docker compose up` fails with "Set POSTGRES_PASSWORD in .env", the `cp .env.example .env`
step was skipped.

### 2. Start the app

In a second terminal:

```bash
cd app/hygiene-tracker-react
cp .env.example .env
npm install
npx expo start --web
```

The web app opens at <http://localhost:8081> and talks to the API at `http://localhost:8080`.

For **Android**, sign-in needs a development build (Expo Go cannot register the app's sign-in
redirect). With an emulator running or a phone connected over USB:

```bash
npx expo run:android
```

The first build takes several minutes; after that, `npx expo start` and press `a`. The Android
emulator reaches the API at `10.0.2.2:8080`; for a physical phone, set `EXPO_PUBLIC_API_BASE_URL`
in the app's `.env` to your computer's LAN address.

### 3. Sign in with GitHub or Google (optional locally)

Sign-in needs OAuth credentials. Without them, the sign-in endpoints return `503` and everything
else works.

- **API** (root `.env`): `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GOOGLE_CLIENT_IDS`.
- **App** (`app/hygiene-tracker-react/.env`): the `EXPO_PUBLIC_*` client IDs. These are public;
  client **secrets** go only in the API's environment.

Ask the team for the values, and see the [app README](app/hygiene-tracker-react/README.md) for the
redirect URIs to register.

---

## Run the tests

### API

Docker must be running: the tests start a throwaway PostgreSQL 18 with Testcontainers.

```bash
cd api
./gradlew build
```

This compiles, runs every test, writes the coverage report to
`api/build/reports/jacoco/test/html/index.html`, and fails if the service layer is below 70%
coverage.

### App

```bash
cd app/hygiene-tracker-react
npm run lint
```

With the API running, `npm run test:api` checks that the app can reach it.

### CI

Every pull request runs the API tests and builds the Docker image in GitHub Actions, and CodeQL
scans the code. A pull request can't merge until its required checks pass. Dependabot opens
weekly pull requests for npm and Gradle updates.

---

## Configuration

Local values live in `.env` (copied from [`.env.example`](.env.example)). `.env` is ignored by
Git; never commit it.

| Variable | Used by | Local value |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | The local database | From `.env.example` |
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | The API (`compose.yml` sets them to the `db` service) | From `.env.example` |
| `JWT_SECRET` | Signing the API's tokens; at least 32 characters or the API won't start | Placeholder in `.env.example` |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GOOGLE_CLIENT_IDS` | Sign-in | Empty (sign-in returns 503) |
| `CORS_ALLOWED_ORIGINS` | Web origins allowed to call the API (comma-separated) | Not set (defaults to `http://localhost:8081`) |
| `PORT` | The port the API listens on | Not set (defaults to `8080`) |

## Deployment

| Part | Where | Notes |
|---|---|---|
| API | Render web service | Built from `api/Dockerfile`; health check `/actuator/health` |
| Web app | Render static site | `npx expo export --platform web` from `app/hygiene-tracker-react`, publishing `dist` |
| Database | Neon (PostgreSQL 18) | Flyway creates and updates the tables when the API starts |

All production values, including secrets, are set only in Render's environment settings.

---

## Documentation

| Document | What it covers |
|---|---|
| [PRD](docs/prd.md) | Users, problem, success measures, scope, user stories and acceptance criteria |
| [ERD](docs/erd.md) | Tables, keys, relationships and what happens on delete |
| [OpenAPI contract](docs/openapi.yaml) | Every endpoint; lint with `npx @redocly/cli lint docs/openapi.yaml` |
| [Error model](docs/error-model.md) | The Problem Details error shape and status codes |
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
