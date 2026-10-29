# ADR-`<NNNN>`: `<short title of the decision>`

[Back to the Project 03 guide](README.md#design-and-documentation)

> An **architecture decision record (ADR)** captures one significant technical decision: the
> situation, what you chose, and what it costs. Write it when you make the decision, not afterward.
>
> Copy this file to `docs/adr/<NNNN>-<short-title>.md` in your team repo (for example
> `docs/adr/0002-web-token-storage.md`), numbering from `0001`. Keep it under one page.
> Once an ADR is **Accepted**, do not rewrite it. To change the decision, write a new ADR that
> supersedes it and update only the old one's **Status** line.

**Status:** `<Proposed | Accepted | Superseded by [ADR-NNNN](NNNN-title.md)>`
**Date:** `<YYYY-MM-DD>`
**Deciders:** `<names (@github)>`
**Supersedes:** `<[ADR-NNNN](NNNN-title.md), or "none">`
**Related PR(s):** `<links to the code PR(s) that implement this decision>`

---

## Context

What problem forced a decision, and what constraints applied at the time? Name the forces:
requirements from the guide, free-tier limits, deadlines, what the team already knows.

`<two to five sentences>`

## Options considered

| Option | Pros | Cons |
|---|---|---|
| `<option A>` | | |
| `<option B>` | | |
| `<option C, if any>` | | |

## Decision

One or two sentences, stated plainly: "We will ..."

`<decision>`

## Consequences

What becomes easier, what becomes harder, and what the team now has to live with or revisit.

- **Good:** `<...>`
- **Bad:** `<...>`
- **Follow-up:** `<issues opened, tests or docs this requires>`
