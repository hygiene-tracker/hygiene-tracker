# Threat model

Five threats to Hygiene Tracker, grouped by STRIDE category. Each mitigation links to the test,
config or workflow that implements it. Mitigations marked **Planned** link to the issue that will
add them.

| # | Threat | STRIDE | Status |
|---|---|---|---|
| 1 | [Cross-user access](#1-information-disclosure-cross-user-access) | Information disclosure | Planned |
| 2 | [Token theft or replay](#2-spoofing-token-theft-or-replay) | Spoofing | In place |
| 3 | [Secret leakage](#3-information-disclosure-secret-leakage) | Information disclosure | In place |
| 4 | [Vulnerable dependencies](#4-tampering-vulnerable-dependencies) | Tampering | In place |
| 5 | [Destructive admin action](#5-elevation-of-privilege-destructive-admin-action) | Elevation of privilege | Planned |

---

## 1. Information disclosure: cross-user access

**Threat:** a signed-in user reads or changes another user's showers or profile by changing an ID
in the URL (an IDOR).

**Mitigations:**
- Every endpoint takes the user's ID from the verified JWT (`sub`), never from the request body or
  query string.
- Owned resources are looked up by ID **and** owner. Another user's resource returns 404, the same
  as one that doesn't exist, so IDs can't be probed.
- **Planned:** ownership tests for each owned resource, starting with showers in PRD-4.2
  ([#38](https://github.com/hygiene-tracker/hygiene-tracker/issues/38)): "Given another user's
  shower, when I request it, then I get Not found."

## 2. Spoofing: token theft or replay

**Threat:** an attacker steals or forges a token and calls the API as someone else.

**Mitigations:**
- The API issues its own JWT, signed with HS256 and a secret of at least 32 bytes. Tokens signed
  with another key or edited after signing are rejected:
  [`AppTokenServiceTest`](../api/src/test/java/com/hygienetracker/api/auth/AppTokenServiceTest.java)
  (`rejectsTokenSignedWithADifferentSecret`, `rejectsTamperedToken`).
- Tokens expire after one hour and must have this API's issuer, which limits how long a stolen
  token works: `issuedTokenCarriesUserIdRolesIssuerAndOneHourExpiry` in the same test, and the
  issuer check in [`JwtConfig`](../api/src/main/java/com/hygienetracker/api/config/JwtConfig.java).
- Sign-in uses Authorization Code with PKCE, and the client secret stays on the API, so an
  intercepted code can't be exchanged by someone else.
- Sign-in rejects unverified emails:
  [`GithubAuthControllerTest`](../api/src/test/java/com/hygienetracker/api/controller/GithubAuthControllerTest.java),
  [`GoogleAuthControllerTest`](../api/src/test/java/com/hygienetracker/api/controller/GoogleAuthControllerTest.java).
- Browsers may call the API only from the deployed web app's origin:
  [`CorsConfigTest`](../api/src/test/java/com/hygienetracker/api/config/CorsConfigTest.java).
- The token is sent in the `Authorization` header, not a cookie, so cross-site request forgery
  doesn't apply. CodeQL's CSRF alert on
  [`SecurityConfig`](../api/src/main/java/com/hygienetracker/api/config/SecurityConfig.java) was
  dismissed for this reason.
- All traffic to Render is HTTPS.
- **Planned:** an ADR on where the web client stores the token (Sprint 3).

## 3. Information disclosure: secret leakage

**Threat:** a database password, JWT secret or OAuth client secret is committed to the public repo.

**Mitigations:**
- gitleaks scans every commit on every pull request and push to `main`, and is a required check:
  [`.github/workflows/gitleaks.yml`](../.github/workflows/gitleaks.yml). One reviewed finding (a
  fake key in an old test) is listed in [`.gitleaksignore`](../.gitleaksignore).
- GitHub secret scanning with push protection is on, so a push containing a known secret format is
  blocked.
- `.env` files are ignored by Git; only [`.env.example`](../.env.example) with placeholders is
  committed: [`.gitignore`](../.gitignore).
- Secrets live only in Render's environment variables. Only public client IDs ship to the app
  (`EXPO_PUBLIC_*`).
- The API refuses to start with a JWT secret under 32 bytes:
  [`JwtConfig`](../api/src/main/java/com/hygienetracker/api/config/JwtConfig.java).

## 4. Tampering: vulnerable dependencies

**Threat:** a library with a known vulnerability is used by the API or the app.

**Mitigations:**
- Dependabot opens weekly update PRs for npm and Gradle, capped with `open-pull-requests-limit`:
  [`.github/dependabot.yml`](../.github/dependabot.yml). Dependabot PRs need an approval and
  passing CI like any other; for example
  [#80](https://github.com/hygiene-tracker/hygiene-tracker/pull/80) (Gradle wrapper) was reviewed
  and merged.
- Dependabot alerts are on. The two alerts found so far (glob and minimatch) were fixed in
  [#70](https://github.com/hygiene-tracker/hygiene-tracker/pull/70).
- CodeQL scans the Java, TypeScript and workflow code on every pull request.

## 5. Elevation of privilege: destructive admin action

**Threat:** a regular user, or an admin whose role was revoked, bans users or revokes admin roles.

**Mitigations:**
- The JWT's `roles` claim is mapped to Spring roles, so admin endpoints can require `ADMIN`:
  [`SecurityConfig`](../api/src/main/java/com/hygienetracker/api/config/SecurityConfig.java).
- Tokens expire after one hour. Once users are stored
  ([#87](https://github.com/hygiene-tracker/hygiene-tracker/issues/87)), roles are read from the
  database each time a token is issued, so a revoked admin loses access within the hour.
- **Planned:** with PRD-7 ([#7](https://github.com/hygiene-tracker/hygiene-tracker/issues/7)),
  tests that a non-admin gets 403 on every admin endpoint and that a revoked admin gets 403 and
  nothing changes.
