import { Platform } from 'react-native';

import type { AuthResponse } from '@/services/google-auth';

export type GithubAuthResponse = AuthResponse;

type ApiError = { error?: string };

const defaultApiHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const apiBaseUrl = (process.env.EXPO_PUBLIC_API_BASE_URL || `http://${defaultApiHost}:8080`).replace(
  /\/$/,
  ''
);

export async function exchangeGithubCode(
  code: string,
  codeVerifier: string,
  redirectUri: string
): Promise<AuthResponse> {
  if (Platform.OS === 'web') {
    throw new Error('GitHub PKCE sign-in is currently configured for the iOS and Android app.');
  }

  const response = await fetch(`${apiBaseUrl}/api/auth/github/exchange`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, codeVerifier, redirectUri }),
  });
  const body = (await response.json().catch(() => ({}))) as GithubAuthResponse | ApiError;
  if (!response.ok) {
    throw new Error(
      'error' in body && body.error ? body.error : `API request failed (${response.status})`
    );
  }
  if (!('token' in body) || typeof body.token !== 'string' || body.tokenType !== 'Bearer' || !body.user) {
    throw new Error('The API returned an unexpected GitHub auth response.');
  }
  return body;
}
