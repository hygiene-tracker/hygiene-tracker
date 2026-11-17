# ADR-0003: Deploy main to Render on every push

**Status:** Accepted
**Date:** 2026-11-11
**Deciders:** Hugo Ruiz-Mireles (@Hugo-RM)
**Supersedes:** none
**Related PR(s):** [#72](https://github.com/hygiene-tracker/hygiene-tracker/pull/72), [#79](https://github.com/hygiene-tracker/hygiene-tracker/pull/79)

## Context

The API (Docker) and web app (static site) needed to be live during Sprint 2 to test sign-in,
CORS and the database together. Render's free tier can deploy a branch automatically.

## Options considered

| Option | Pros | Cons |
|---|---|---|
| Auto-deploy `main` on every push | Nothing to set up; every merge is live within minutes | Production can change without a release |
| Deploy only from tagged releases | Production always matches a release; easy rollback | Needs a deploy step at each release |

## Decision

For now, Render deploys `main` automatically on every merge.

## Consequences

- **Good:** fast feedback while the deployment is new.
- **Bad:** the course requires production to deploy only from tagged releases, which this doesn't
  meet.
- **Follow-up:** switch to deploying from release tags at the end of Sprint 2 and record it in a new
  ADR that supersedes this one ([#75](https://github.com/hygiene-tracker/hygiene-tracker/issues/75)).
