# ADR-0001: PostgreSQL on Neon

**Status:** Accepted
**Date:** 2026-11-10
**Deciders:** Hugo Ruiz-Mireles (@Hugo-RM), Jaime Serrano Acevedo (@jai-serr-ace), Leonardo Lopez (@leodebuss)
**Supersedes:** none
**Related PR(s):** [#63](https://github.com/hygiene-tracker/hygiene-tracker/pull/63), [#79](https://github.com/hygiene-tracker/hygiene-tracker/pull/79)

## Context

The API needs a hosted PostgreSQL database on a free tier. The course allows Supabase or Neon for
the database only, and warns that Render's free Postgres expires after 30 days. Locally and in
tests we already run PostgreSQL 18 with Flyway migrations.

## Options considered

| Option | Pros | Cons |
|---|---|---|
| Neon | Plain Postgres 18, no expiry, same region as Render (Oregon) | Compute suspends when idle, so the first query can be slow |
| Supabase | Plain Postgres, generous free tier | Bundles auth and APIs we must not use; more to configure |
| Render Postgres | Same provider as the API | Free database expires after 30 days |

## Decision

We will host the database on Neon (Postgres 18, AWS us-west-2), connect with the direct (not
pooled) connection string, and let Flyway own the schema.

## Consequences

- **Good:** production matches local and test databases (Postgres 18 with the same migrations).
- **Bad:** the first request after Neon suspends is slower.
- **Follow-up:** credentials live only in Render and a password manager; never create tables in
  Neon's console, since Flyway would refuse to run.
