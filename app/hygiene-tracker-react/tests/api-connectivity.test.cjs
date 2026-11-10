const assert = require('node:assert/strict');
const { test } = require('node:test');

const apiBaseUrl = (process.env.API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

test('Spring Boot API is reachable without an auth token', async () => {
  let response;
  try {
    response = await fetch(`${apiBaseUrl}/api/health`, {
      signal: AbortSignal.timeout(5000),
    });
  } catch (error) {
    assert.fail(
      `Could not reach ${apiBaseUrl}. Start the Spring Boot API or set API_BASE_URL. ${error.message}`,
    );
  }

  assert.equal(response.status, 200, `Expected HTTP 200, got ${response.status}`);
  assert.match(response.headers.get('content-type') || '', /application\/json/i);

  const body = await response.json();
  assert.equal(body.status, 'UP');
});
