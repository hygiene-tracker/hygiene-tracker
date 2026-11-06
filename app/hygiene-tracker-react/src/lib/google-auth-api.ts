const MOCK_GOOGLE_ID_TOKEN = 'mock-google-id-token';
const MOCK_AUTH_ENDPOINT = 'https://api.example.com/auth/google';

export async function sendGoogleIdToken(
  idToken = MOCK_GOOGLE_ID_TOKEN,
  endpoint = MOCK_AUTH_ENDPOINT,
): Promise<unknown> {
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ idToken }),
  });

  if (!response.ok) {
    throw new Error(`Google authentication request failed with status ${response.status}.`);
  }

  return response.json();
}

export { MOCK_AUTH_ENDPOINT, MOCK_GOOGLE_ID_TOKEN };
