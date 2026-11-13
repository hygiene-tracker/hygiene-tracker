import { makeRedirectUri } from 'expo-auth-session';

import type { AuthResponse } from '@/services/google-auth';

export type GithubAuthResponse = AuthResponse;

type ProblemResponse = { detail?: string };

/** Use the callback registered in GitHub, with Expo's generated callback as the fallback. */
export const githubRedirectUri =
  process.env.EXPO_PUBLIC_GITHUB_REDIRECT_URI?.trim() ||
  makeRedirectUri({
    scheme: 'hygiene-tracker',
    path: 'auth/github',
  });

const apiBaseUrl = (process.env.EXPO_PUBLIC_API_BASE_URL || 'https://hygiene-tracker.onrender.com').replace(
  /\/$/,
  ''
);

export async function exchangeGithubCode(
  code: string,
  codeVerifier: string,
  redirectUri: string
): Promise<AuthResponse> {
  const endpoint = `${apiBaseUrl}/api/auth/github`;
  console.info('[GitHub OAuth] Exchanging authorization code', {
    endpoint,
    redirectUri,
    // Deliberately do not log code or codeVerifier.
  });

  let response: Response;
  try {
    response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, codeVerifier, redirectUri }),
    });
  } catch (error: unknown) {
    console.error('[GitHub OAuth] API request could not reach the server', {
      endpoint,
      error: error instanceof Error ? error.message : error,
    });
    throw error;
  }

  const body = (await response.json().catch(() => ({}))) as GithubAuthResponse | ProblemResponse;
  console.info('[GitHub OAuth] API response', {
    status: response.status,
    ok: response.ok,
    detail: 'detail' in body ? body.detail : undefined,
  });
  if (!response.ok) {
    throw new Error(
      'detail' in body && body.detail ? body.detail : `API request failed (${response.status})`
    );
  }
  if (!('token' in body) || typeof body.token !== 'string' || body.tokenType !== 'Bearer' || !body.user) {
    throw new Error('The API returned an unexpected GitHub auth response.');
  }
  return body;
}
