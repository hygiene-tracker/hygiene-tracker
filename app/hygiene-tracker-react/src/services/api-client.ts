import { clearAuth, loadAccessToken } from '@/services/auth';

const apiBaseUrl = (process.env.EXPO_PUBLIC_API_BASE_URL || 'https://hygiene-tracker.onrender.com').replace(
  /\/$/,
  ''
);

/** Sends an API request with the saved JWT when one is available. */
export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const token = await loadAccessToken();
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers });
  if (response.status === 401) await clearAuth();
  return response;
}
