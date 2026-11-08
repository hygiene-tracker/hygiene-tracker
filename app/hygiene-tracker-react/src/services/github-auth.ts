import * as WebBrowser from 'expo-web-browser';
import { Platform } from 'react-native';

export type VerifiedGithubUser = {
  id: number;
  login: string;
  name: string | null;
  email: string | null;
  emailVerified: boolean;
  avatar_url: string | null;
};

type GithubAuthResponse = {
  valid: boolean;
  user: VerifiedGithubUser;
};

type ApiError = { error?: string };

const defaultApiHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const apiBaseUrl = (process.env.EXPO_PUBLIC_API_BASE_URL || `http://${defaultApiHost}:8080`).replace(
  /\/$/,
  ''
);
const appRedirectUri = 'hygiene-tracker://auth/github';

export async function signInWithGithub(): Promise<VerifiedGithubUser> {
  if (Platform.OS === 'web') {
    throw new Error('GitHub sign-in handoff is currently configured for the iOS and Android app.');
  }

  const result = await WebBrowser.openAuthSessionAsync(
    `${apiBaseUrl}/api/auth/github/login`,
    appRedirectUri
  );
  if (result.type !== 'success') {
    throw new Error('GitHub sign-in was canceled or dismissed.');
  }

  const redirectUrl = new URL(result.url);
  const error = redirectUrl.searchParams.get('error');
  if (error) {
    throw new Error(`GitHub sign-in failed (${error}).`);
  }
  const handoffId = redirectUrl.searchParams.get('handoffId');
  if (!handoffId) {
    throw new Error('GitHub returned without a sign-in result.');
  }

  const response = await fetch(`${apiBaseUrl}/api/auth/github/result`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ handoffId }),
  });
  const body = (await response.json().catch(() => ({}))) as GithubAuthResponse | ApiError;
  if (!response.ok) {
    throw new Error(
      'error' in body && body.error ? body.error : `API request failed (${response.status})`
    );
  }
  if (!('valid' in body) || !body.valid || !body.user) {
    throw new Error('The API returned an unexpected GitHub auth response.');
  }
  return body.user;
}
