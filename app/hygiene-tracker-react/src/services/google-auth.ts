import { Platform } from 'react-native';

export type AuthUser = {
  id: number;
  email: string;
  displayName: string | null;
  avatar: string | null;
  role: 'USER' | 'ADMIN';
  createdAt: string;
};

export type AuthResponse = {
  token: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: AuthUser;
};

const defaultApiHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL || `http://${defaultApiHost}:8080`;

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
