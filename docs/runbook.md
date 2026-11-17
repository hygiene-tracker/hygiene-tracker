# Runbook

How to operate Hygiene Tracker in production:
- deploy
- roll back
- rotate a secret
- check health
- find logs
- handle an unreachable database

For running it locally, see the [README](../README.md).

| Part | Where | Name |
|---|---|---|
| API (Spring Boot, Docker) | Render web service | `hygiene-tracker` |
| Web app (Expo web build) | Render static site | `hygiene-tracker-web` |
| Database (PostgreSQL 18) | Neon | project `hygiene-tracker`, database `neondb` |

Only the team has access to Render, Neon, GitHub's OAuth app and Google Cloud. Secret values live
only in Render's environment settings and the team's password manager, never in the repo or chat.

---

## Deploy

Production deploys only from a tagged GitHub release (`v0.3.0`, `v0.4.0`, ...), cut by the
sprint's merge captain. Until `v0.3.0` is released, Render still deploys `main` on every merge.

1. The captain publishes the release: GitHub, **Releases**, **Draft a new release**, new tag on
   `main`, **Generate release notes**, **Publish**.
2. In Render, open `hygiene-tracker`, then **Manual Deploy**, **Deploy a specific commit**, and pick
   the release tag's commit.
3. Do the same for `hygiene-tracker-web`.
4. Check:
   - <https://hygiene-tracker.onrender.com/actuator/health> returns `{"status":"UP"}`
     (the first request can take a few minutes if Render was asleep)
   - <https://hygiene-tracker-web.onrender.com> loads

Flyway applies any new database migrations when the API starts; nothing is run by hand.

## Roll back

Use this when a release breaks production.

1. Find the previous release on GitHub's **Releases** page and its commit.
2. In Render, open the broken service, then **Manual Deploy**, **Deploy a specific commit**, and pick
   the previous release's commit. (Or, under **Events**, use **Rollback** on the last good deploy.)
3. Repeat for the other service if it was also updated.
4. Check health and the web app as in [Deploy](#deploy).
5. To go forward again after a fix, deploy the new release's commit the same way.

**Database:** migrations only move forward. Rolling back the code is safe when the newer release
only *added* tables or columns. If it removed or renamed one, the older code may fail Hibernate's
schema check at startup; fix forward with a new release instead.

## Rotate a secret

Rotate a secret as soon as it may have leaked (committed, pasted in chat, shown on screen). Removing
it from Git isn't enough, because the history is public.

| Secret | Where to create the new value | Then |
|---|---|---|
| `JWT_SECRET` | Run `openssl rand -base64 48` | Everyone is signed out; they sign in again |
| `SPRING_DATASOURCE_PASSWORD` | Neon, **Roles**, `neondb_owner`, **Reset password** | The old password stops working immediately |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth app, **Generate a new client secret** | Delete the old secret in the OAuth app |
| `GOOGLE_WEB_CLIENT_SECRET` | Google Cloud, **Credentials**, the web client, **Reset secret** | |

For each one:

1. Create the new value as shown above.
2. In Render, open `hygiene-tracker`, **Environment**, replace the value, and **Save**. Render
   redeploys the API with it.
3. Update the team password manager.
4. Check health, and that sign-in still works on the web app.
5. If the secret was committed, open an issue noting it was rotated, and check that the gitleaks
   check would have caught it.

Client IDs (`GITHUB_CLIENT_ID`, `GOOGLE_*_CLIENT_ID`, `EXPO_PUBLIC_*`) are public and don't need
rotating.

## Check health

| Where | URL |
|---|---|
| Production | <https://hygiene-tracker.onrender.com/actuator/health> |
| Local stack | <http://localhost:8080/actuator/health> |

A healthy API answers HTTP `200` with:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

An unhealthy one answers HTTP `503` with `"status":"DOWN"`, usually because the database can't be
reached (see [Database unreachable](#database-unreachable)). No response at all means the API isn't
running: on Render it may be asleep (wait a few minutes) or failed to start (check **Events** and
**Logs**).

## Find logs

| What | Where |
|---|---|
| API startup, errors and warnings | Render, `hygiene-tracker`, **Logs** (searchable) |
| Deploys, restarts, health check failures | Render, `hygiene-tracker`, **Events** |
| Web app build | Render, `hygiene-tracker-web`, **Events**, the deploy's build log |
| Database activity and limits | Neon, project `hygiene-tracker`, **Monitoring** |
| Local stack | `docker compose logs api` (add `-f` to follow) |

Every error response includes a `requestId` (see the [error model](error-model.md)). Search for it
in the Render logs to find that request.

## Database unreachable

**What users see:** pages that need data show an error, and the API returns `500` or `503`
responses. `/actuator/health` returns `{"status":"DOWN"}` with HTTP `503`.

**Not an outage:** Neon's free tier suspends the database after 5 minutes idle. The next query
wakes it in a few seconds, so a short delay is normal.

1. Check health: <https://hygiene-tracker.onrender.com/actuator/health>.
2. Check the Render logs for connection errors (`Connection refused`, `password authentication
   failed`, `too many connections`).
3. Check Neon: is the project active, or has it hit the free-tier compute limit? Check
   <https://neonstatus.com> for an incident.
4. If the password was changed, update `SPRING_DATASOURCE_PASSWORD` in Render (see
   [Rotate a secret](#rotate-a-secret)).
5. When Neon is back, restart the API (Render, **Manual Deploy**, **Restart service**) if health
   doesn't return to `UP` on its own, then check health again.
