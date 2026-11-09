# Error Model

Every error response from the Hygiene Tracker API uses one shape: [RFC 9457 Problem
Details][rfc9457], sent with the media type `application/problem+json`. It is the `Problem`
schema in `docs/openapi.yaml`, and every error response in the contract refers to it.

One shape means the web and Android clients handle every error in one place, and the tests
check status codes and messages the same way on every endpoint.

## The shape

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Duration must be 1–120 minutes",
  "instance": "/showers",
  "requestId": "3f2a9c1e-6b7d-4e21-9a0c-5d8e2f1b4c6a"
}
```

| Field | Always present | Meaning |
|---|---|---|
| `type` | Yes | `about:blank`: the status code is the error type |
| `title` | Yes | The standard reason phrase for the status code, such as `Not Found` |
| `status` | Yes | The HTTP status code, repeated in the body |
| `detail` | Yes | What went wrong, written for the user. The clients show this text |
| `instance` | Yes | The request path that failed |
| `requestId` | Yes | The same ID as the request's log line, so an error a user reports can be found in the logs |
| `errors` | Only for validation errors | One entry per invalid field: `{ "field": "...", "message": "..." }` |

`requestId` and `errors` are extension members, which RFC 9457 allows.

### A validation error with field details

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Display name is required",
  "instance": "/me",
  "requestId": "8c1d4e7f-2a3b-4c5d-9e6f-0a1b2c3d4e5f",
  "errors": [
    { "field": "displayName", "message": "Display name is required" }
  ]
}
```

When one field is invalid, `detail` is that field's message. When several are, `detail` is
the first message and `errors` lists all of them.

## Status codes

| Status | When | Example `detail` | Source |
|---|---|---|---|
| **400** Bad Request | Malformed JSON, a failed validation rule, or a bad query parameter | "Duration must be 1–120 minutes" | [PRD-4][prd] |
| | | "Display name is required" | [PRD-2][prd] |
| | Sign-in with an email the provider has not verified (nothing is saved) | "Verify your email with GitHub, then sign in again" | [Appendix I, 3.5][appendix-email-rule] |
| **401** Unauthorized | No token, an invalid token, or an expired token | "Sign in to continue" | |
| **403** Forbidden | Signed in, but the action needs a role the user does not have, such as an admin action by a user | "You don't have permission to do that" | [PRD-7][prd] |
| **404** Not Found | The resource does not exist, **or it belongs to another user** | "Not found" | [PRD-4][prd] |
| **409** Conflict | The request conflicts with existing data, such as signing in with an email already registered through the other provider (nothing is saved) | "This email is registered with GitHub" | [PRD-1][prd], [Appendix I, 3.5][appendix-email-rule] |
| **500** Internal Server Error | An unexpected error on our side | "Something went wrong. Try again later." | |
| **503** Service Unavailable | The database or a provider (GitHub, Google) cannot be reached | "The service is temporarily unavailable. Try again in a minute." | Runbook: database unreachable |

The PRD messages above are part of the contract: the API returns them word for word, and the
tests check them word for word.

## Rules

- **Another user's resource returns 404, not 403.** A 403 would confirm the resource exists.
  The ownership (IDOR) tests check this for every owned resource.
- **403 is only for missing roles.** The user is known, and the action is not allowed for
  their role. Admin checks use the role stored in the database, so a revoked admin gets 403
  even with a token issued before the role was revoked.
- **401 versus 403:** 401 means the API does not know who you are; 403 means it does, and the
  answer is no.
- **A failed request changes nothing.** A 400, 403, 404 or 409 response means no data was
  created, changed or deleted.
- **Error bodies never contain** stack traces, SQL, class names, tokens, secrets or another
  user's data. A 500 always uses the generic `detail` above; the real cause goes only in the
  server log, under the same `requestId`.

## How the API produces it

- `spring.mvc.problemdetails.enabled=true` makes Spring send Problem Details for its own errors,
  such as malformed JSON or a wrong HTTP method ([Spring reference][spring-problem-details]).
- Services signal errors with exceptions such as `ResponseStatusException` or a small set of
  our own (for example `NotFoundException`). One `@RestControllerAdvice` turns them into this
  shape.
- That advice also maps validation failures to the exact PRD messages, filling `detail` and
  `errors`. Spring's built-in validation message ("Invalid request content.") is never sent.
- Spring Security answers 401 and 403 before a request reaches a controller, and by default with
  an empty body. A custom `AuthenticationEntryPoint` and `AccessDeniedHandler` send this shape
  instead, so every endpoint follows the same model.
- A request filter assigns `requestId`, writes it to the request's log line, and returns it in
  the `X-Request-Id` response header as well as in error bodies.

## How the clients use it

The web and Android clients handle errors in one shared function in the API module:

| Status | Client behavior |
|---|---|
| 400, 409 | Show `detail` next to the form, or `errors` next to each field |
| 401 | Clear the stored token and go to the sign-in screen |
| 403 | Show "You don't have permission to do that" |
| 404 | Show a "Not found" state instead of the screen's content |
| 500, 503 | Show "Something went wrong" with a retry button |

Each of these is a graded error state for the web client (401, 403, 404 and 500).

[rfc9457]: https://www.rfc-editor.org/rfc/rfc9457
[spring-problem-details]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
[prd]: prd.md
[appendix-email-rule]: https://github.com/CST-438-projects/cst438-docs/blob/main/projects/p3/appendix_i_auth.md#35-user-table-and-the-email-rule-linking-is-a-stretch-goal
