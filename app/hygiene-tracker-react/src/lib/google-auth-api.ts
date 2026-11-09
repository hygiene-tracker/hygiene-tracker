const MOCK_GOOGLE_ID_TOKEN = 'mock-google-id-token';
const MOCK_AUTH_ENDPOINT = 'https://api.example.com/api/auth/google';

export type GoogleAuthContractResponse = {
  token: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: {
    id: number;
    email: string;
    displayName: string | null;
    avatar: string | null;
    role: 'USER' | 'ADMIN';
    createdAt: string;
  };
};

export async function sendGoogleIdToken(
  idToken = MOCK_GOOGLE_ID_TOKEN,
  endpoint = MOCK_AUTH_ENDPOINT,
): Promise<GoogleAuthContractResponse> {
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ idToken }),
  });

  const body = (await response.json().catch(() => ({}))) as
    | GoogleAuthContractResponse
    | { detail?: string };

  if (!response.ok) {
    throw new Error(
      ('detail' in body && body.detail) ||
        `Google authentication request failed with status ${response.status}.`,
    );
  }

  return body as GoogleAuthContractResponse;
}

export { MOCK_AUTH_ENDPOINT, MOCK_GOOGLE_ID_TOKEN };
