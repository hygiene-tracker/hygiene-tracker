const assert = require('node:assert/strict');
const { test } = require('node:test');

// Defaults to the local stack from `docker compose up`. Set API_BASE_URL to check the hosted API.
const apiBaseUrl = (process.env.API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
// The hosted API on Render's free tier can take a few minutes to wake up.
const timeoutMs = apiBaseUrl.includes('localhost') ? 5000 : 180000;
const browserOrigin = process.env.FRONTEND_ORIGIN || 'http://localhost:8081';

async function request(path, options = {}) {
  try {
    return await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      signal: options.signal || AbortSignal.timeout(timeoutMs),
    });
  } catch (error) {
    assert.fail(
      `Could not reach ${apiBaseUrl}. Start the Spring Boot API or set API_BASE_URL. ${error.message}`,
    );
  }
}

/**
 * A fake sign-in must be rejected the way docs/openapi.yaml says: 400 when sign-in is configured
 * (the fake value is rejected), or 503 "not configured" when OAuth credentials aren't set (a normal
 * local run). Anything else, including a 500, is a bug.
 */
async function assertRejectedSignIn(response, provider) {
  assert.match(response.headers.get('content-type') || '', /application\/problem\+json/);
  const problem = await response.json();
  if (response.status === 503) {
    assert.match(problem.detail, /not configured/, `${provider} returned 503: ${problem.detail}`);
  } else {
    assert.equal(response.status, 400, `${provider} returned ${response.status}: ${problem.detail}`);
  }
}

test('Spring Boot API is reachable without an auth token', async () => {
  const response = await request('/api/health');

  assert.equal(response.status, 200, `Expected HTTP 200, got ${response.status}`);
  assert.match(response.headers.get('content-type') || '', /application\/json/i);

  const body = await response.json();
  assert.equal(body.status, 'UP');
});

test('API allows the local frontend origin through CORS', async () => {
  const response = await request('/api/auth/google', {
    method: 'OPTIONS',
    headers: {
      Origin: browserOrigin,
      'Access-Control-Request-Method': 'POST',
      'Access-Control-Request-Headers': 'content-type',
    },
  });

  assert.ok([200, 204].includes(response.status), `Expected CORS preflight success, got ${response.status}`);
  assert.equal(response.headers.get('access-control-allow-origin'), browserOrigin);
});

test('Google sign-in rejects a fake ID token with a Problem response', async () => {
  const response = await request('/api/auth/google', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken: 'connection-test-invalid-token' }),
  });

  await assertRejectedSignIn(response, 'Google');
});

test('GitHub sign-in rejects a fake code with a Problem response', async () => {
  const response = await request('/api/auth/github', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      code: 'connection-test-invalid-code',
      codeVerifier: 'connection-test-verifier',
      redirectUri: 'http://localhost:8081/auth/github',
    }),
  });

  await assertRejectedSignIn(response, 'GitHub');
});
