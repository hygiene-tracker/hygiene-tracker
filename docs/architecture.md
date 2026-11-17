# Architecture

How Hygiene Tracker fits together, as C4 context and container diagrams, plus the sign-in
flow. Decisions behind it are in [`docs/adr/`](adr/).

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

## 3. Sign-in flow

How sign-in works from the button to a protected request, and where each error comes from
([error model](error-model.md)). The app signs in with GitHub or Google, and the API exchanges the
result for its own JWT ([ADR-0002](adr/0002-backend-token-exchange.md)). Which part talks to which
is in the [container diagram](#2-containers).

```mermaid
flowchart TD
    start([User taps Sign in]) --> provider{Choose provider}

    provider --> github[GitHub: Authorization Code + PKCE]
    github --> githubExchange[API exchanges code + verifier with GitHub]
    githubExchange --> email[API gets the verified email]

    provider --> google[Google sign-in]
    google --> clientType{Client type}
    clientType -->|Web| googleCode[App sends authorization code;<br/>API exchanges it with Google]
    clientType -->|Android| googleToken[App sends Google ID token]
    googleCode --> googleVerify[API verifies the Google ID token]
    googleToken --> googleVerify
    googleVerify --> email

    githubExchange -. code invalid or expired .-> badRequest[400 Bad Request]
    googleVerify -. token invalid .-> badRequest
    githubExchange -. provider unreachable or not configured .-> unavailable[503 Service Unavailable]
    googleVerify -. provider unreachable or not configured .-> unavailable

    email --> linked{Identity already linked?}
    linked -->|Yes| jwt
    linked -->|No| verified{Verified email?}
    verified -->|No| badRequest
    verified -->|Yes| conflict{Email registered with<br/>the other provider?}
    conflict -->|Yes| conflictError[409 Conflict<br/>no account created]
    conflict -->|No| create[Create user and identity]
    create --> jwt[API issues JWT, 1-hour lifetime]

    jwt --> store[App stores the JWT]
    store --> request[App sends Authorization: Bearer JWT]
    request --> valid{JWT valid and unexpired?}
    valid -->|No| unauthorized[401 Unauthorized]
    valid -->|Yes| resource[API serves the request]
```
