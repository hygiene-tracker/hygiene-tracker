# Architecture

How Hygiene Tracker fits together, as C4 context and container diagrams, plus the sign-in
sequence. Decisions behind it are in [`docs/adr/`](adr/).

## 1. Context

Who uses the system and what it depends on.

```mermaid
flowchart LR
    user(["User<br/>logs showers, checks score and leaderboard"])
    admin(["Admin<br/>bans users, revokes admin role"])
    system["Hygiene Tracker<br/>web and Android app + REST API"]
    github["GitHub<br/>OAuth sign-in"]
    google["Google<br/>OAuth sign-in"]

    user -- "uses" --> system
    admin -- "uses" --> system
    system -- "verifies sign-in" --> github
    system -- "verifies sign-in" --> google
```

## 2. Containers

What runs where. Everything in the dashed box is ours.

```mermaid
flowchart TB
    user(["User / Admin"])

    subgraph ht["Hygiene Tracker"]
        web["Web client<br/>Expo web build<br/>Render static site"]
        android["Android client<br/>Expo development build"]
        api["REST API<br/>Spring Boot 4, Java 21, Docker<br/>Render web service"]
        db[("Database<br/>PostgreSQL 18 on Neon<br/>schema by Flyway")]
    end

    github["GitHub OAuth"]
    google["Google OAuth"]

    user --> web
    user --> android
    web -- "HTTPS, JSON, Bearer JWT" --> api
    android -- "HTTPS, JSON, Bearer JWT" --> api
    web -- "sign in (PKCE)" --> github
    web -- "sign in" --> google
    android -- "sign in (PKCE)" --> github
    android -- "sign in" --> google
    api -- "exchange code, read verified email" --> github
    api -- "verify ID token" --> google
    api -- "JDBC (TLS)" --> db
```

| Container | Technology | Hosted on |
|---|---|---|
| Web client | React Native with Expo, TypeScript | Render static site |
| Android client | Same codebase as the web client | Installed on the device |
| REST API | Spring Boot 4, Spring Security (OAuth2 Resource Server), Docker | Render web service |
| Database | PostgreSQL 18, Flyway migrations | Neon |

## 3. Sign-in sequence

The app signs in with the provider, and the API exchanges the result for its own JWT
([ADR-0002](adr/0002-backend-token-exchange.md)). GitHub is shown; Google is the same except the
app sends a Google ID token to `POST /api/auth/google`, which the API verifies directly.

```mermaid
sequenceDiagram
    actor U as User
    participant C as App (web or Android)
    participant GH as GitHub
    participant API as REST API
    participant DB as Database

    U->>C: Tap "Sign in with GitHub"
    C->>GH: Authorize (client ID, PKCE code challenge)
    GH-->>U: Sign in and approve
    GH-->>C: Redirect with authorization code
    C->>API: POST /api/auth/github {code, codeVerifier, redirectUri}
    API->>GH: Exchange code + verifier + client secret for an access token
    GH-->>API: Access token
    API->>GH: Get primary verified email
    GH-->>API: Email
    API->>DB: Find or create the user and identity
    API-->>C: 200 {token (our JWT, 1 hour), user}
    C->>API: Later requests with Authorization: Bearer <JWT>
    API-->>C: 401 if the token is missing, expired or invalid
```

Unverified emails get 400, and an email already registered with the other provider gets 409
([error model](error-model.md)). Saving users on sign-in (the "find or create" step) is
[#87](https://github.com/hygiene-tracker/hygiene-tracker/issues/87).
