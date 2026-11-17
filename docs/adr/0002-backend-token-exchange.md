# ADR-0002: Sign-in by backend token exchange

**Status:** Accepted
**Date:** 2026-11-10
**Deciders:** Hugo Ruiz-Mireles (@Hugo-RM), Jaime Serrano Acevedo (@jai-serr-ace), Leonardo Lopez (@leodebuss)
**Supersedes:** none
**Related PR(s):** [#46](https://github.com/hygiene-tracker/hygiene-tracker/pull/46), [#48](https://github.com/hygiene-tracker/hygiene-tracker/pull/48), [#63](https://github.com/hygiene-tracker/hygiene-tracker/pull/63)

## Context

Users sign in with GitHub or Google on web and Android. GitHub issues no ID token, so the API
can't verify a GitHub sign-in on its own. Appendix I offers a third-party auth broker or a
backend token exchange; the broker costs points.

## Options considered

| Option | Pros | Cons |
|---|---|---|
| Backend token exchange (Appendix I option b) | One token format for both providers; client secret stays on the API | We write and test the exchange and JWT code |
| Auth broker (e.g. Supabase Auth, Firebase) | Less code | Costs points; another service holds our users |
| Use each provider's token directly | No token of our own | GitHub has no ID token; two token formats to check |

## Decision

We will have the app sign in with PKCE and send the result to `POST /api/auth/github` or
`/api/auth/google`. The API verifies it with the provider and returns its own JWT (HS256, one-hour
expiry, `roles` claim), which every other endpoint requires.

## Consequences

- **Good:** one token for both providers; the GitHub client secret never ships in the app.
- **Bad:** we own the JWT code, its secret, and its tests.
- **Follow-up:** store users on sign-in ([#87](https://github.com/hygiene-tracker/hygiene-tracker/issues/87)); decide where the web client keeps the token (Sprint 3).
