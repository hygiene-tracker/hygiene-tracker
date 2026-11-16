const assert = require('node:assert/strict');
const { test } = require('node:test');

const apiBaseUrl = (process.env.API_BASE_URL || 'https://hygiene-tracker.onrender.com').replace(/\/$/, '');
const browserOrigin = process.env.FRONTEND_ORIGIN || 'http://localhost:8081';

async function request(path, options = {}) {
  try {
    return await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      signal: options.signal || AbortSignal.timeout(5000),
    });
  } catch (error) {
    assert.fail(
      `Could not reach ${apiBaseUrl}. Start the Spring Boot API or set API_BASE_URL. ${error.message}`,
    );
  }
}

test('Spring Boot API is reachable without an auth token', async () => {
  const response = await request('/api/health');

  assert.equal(response.status, 200, `Expected HTTP 200, got ${response.status}`);
  assert.match(response.headers.get('content-type') || '', /application\/json/i);

  const body = await response.json();
  assert.equal(body.status, 'UP');
});

test('Hosted API allows the local frontend origin through CORS', async () => {
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

test('Google OAuth route is reachable from the frontend API base URL', async () => {
  const response = await request('/api/auth/google', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken: 'connection-test-invalid-token' }),
  });

  assert.ok(
    [400, 401, 403].includes(response.status),
    `Expected OAuth validation failure, got ${response.status}`,
  );
});

test('GitHub OAuth route is reachable from the frontend API base URL', async () => {
  const response = await request('/api/auth/github', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      code: 'connection-test-invalid-code',
      codeVerifier: 'connection-test-verifier',
      redirectUri: 'http://localhost:8081/auth/github',
    }),
  });

  assert.ok(
    [400, 401, 403, 500].includes(response.status),
    `Expected GitHub OAuth validation failure, got ${response.status}`,
  );
});
