import type { AuthResponse } from '@/services/auth';

export type { AuthResponse, AuthUser } from '@/services/auth';

const apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL || 'https://hygiene-tracker.onrender.com';

export async function signInWithGoogle(idToken: string): Promise<AuthResponse> {
  const response = await fetch(`${apiBaseUrl.replace(/\/$/, '')}/api/auth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken }),
  });

  const body = (await response.json().catch(() => ({}))) as AuthResponse | ProblemResponse;

  if (!response.ok) {
    throw new Error('detail' in body && body.detail ? body.detail : `API request failed (${response.status})`);
  }

  if (!isAuthResponse(body)) {
    throw new Error('The API returned an unexpected Google auth response');
  }

  return body;
}

type ProblemResponse = {
  detail?: string;
};

function isAuthResponse(body: AuthResponse | ProblemResponse): body is AuthResponse {
  return (
    'token' in body &&
    typeof body.token === 'string' &&
    body.tokenType === 'Bearer' &&
    typeof body.expiresIn === 'number' &&
    Boolean(body.user)
  );
}

export const validateGoogleIdToken = async (idToken: string) =>
  (await signInWithGoogle(idToken)).user;
