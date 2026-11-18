import { clearAuth, loadAccessToken } from '@/services/auth';

const apiBaseUrl = (process.env.EXPO_PUBLIC_API_BASE_URL || 'https://hygiene-tracker.onrender.com').replace(
  /\/$/,
  ''
);
const debugApi = process.env.EXPO_PUBLIC_DEBUG_API === 'true';

function logApi(message: string, details: Record<string, unknown>): void {
  if (debugApi) console.info(`[api] ${message}`, details);
}

/** Sends an API request with the saved JWT when one is available. */
export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const token = await loadAccessToken();
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const method = (init.method || 'GET').toUpperCase();
  logApi('request', { method, path });
  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers });
  } catch (error: unknown) {
    if (debugApi) console.warn('[api] network error', { method, path });
    throw error;
  }
  logApi('response', {
    method,
    path,
    status: response.status,
    contentType: response.headers.get('content-type') || 'unknown',
  });
  if (response.status === 401) await clearAuth();
  return response;
}
