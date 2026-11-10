import { Platform } from 'react-native';

export type VerifiedGoogleUser = {
  subject: string;
  email: string | null;
  emailVerified: boolean | null;
  name: string | null;
  picture: string | null;
};

type GoogleAuthResponse = {
  valid: boolean;
  user: VerifiedGoogleUser;
};

const defaultApiHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL || `http://${defaultApiHost}:8080`;

export async function validateGoogleIdToken(idToken: string): Promise<VerifiedGoogleUser> {
  const response = await fetch(`${apiBaseUrl.replace(/\/$/, '')}/api/auth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken }),
  });

  const body = (await response.json().catch(() => ({}))) as
    | GoogleAuthResponse
    | { error?: string };

  if (!response.ok) {
    throw new Error(
      'error' in body && body.error ? body.error : `API request failed (${response.status})`
    );
  }

  if (!('valid' in body) || !body.valid || !body.user) {
    throw new Error('The API returned an unexpected Google auth response');
  }

  return body.user;
}
