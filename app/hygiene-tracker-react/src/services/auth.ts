import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const AUTH_STORAGE_KEY = 'hygiene-tracker.auth-response';
let webAuth: AuthResponse | null = null;

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

function isAuthResponse(value: unknown): value is AuthResponse {
  if (!value || typeof value !== 'object') return false;

  const response = value as Partial<AuthResponse>;
  return (
    typeof response.token === 'string' &&
    response.token.length > 0 &&
    response.tokenType === 'Bearer' &&
    typeof response.expiresIn === 'number' &&
    Number.isFinite(response.expiresIn) &&
    !!response.user &&
    typeof response.user === 'object'
  );
}

/** Stores the API auth response in the device's platform-protected secure store. */
export async function saveAuth(response: AuthResponse): Promise<void> {
  if (Platform.OS === 'web') {
    // Web has no equivalent of the native keychain/Android Keystore. Keep the
    // token in memory rather than putting it in persistent browser storage.
    webAuth = response;
    return;
  }
  await SecureStore.setItemAsync(AUTH_STORAGE_KEY, JSON.stringify(response));
}

/** Loads the saved auth response, removing malformed data if it is encountered. */
export async function loadAuth(): Promise<AuthResponse | null> {
  if (Platform.OS === 'web') return webAuth;

  const serialized = await SecureStore.getItemAsync(AUTH_STORAGE_KEY);
  if (!serialized) return null;

  try {
    const parsed: unknown = JSON.parse(serialized);
    if (isAuthResponse(parsed)) return parsed;
  } catch {
    // Treat invalid stored data as a signed-out state.
  }

  await clearAuth();
  return null;
}

export async function clearAuth(): Promise<void> {
  if (Platform.OS === 'web') {
    webAuth = null;
    return;
  }
  await SecureStore.deleteItemAsync(AUTH_STORAGE_KEY);
}

export async function loadAccessToken(): Promise<string | null> {
  return (await loadAuth())?.token ?? null;
}
