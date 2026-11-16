import { tokenStore } from '@/lib/token-store';

// Production builds run on Render and call the Render API unless a local Docker URL is supplied
// through EXPO_PUBLIC_API_BASE_URL.
const apiBaseUrl = (process.env.EXPO_PUBLIC_API_BASE_URL || 'https://hygiene-tracker.onrender.com').replace(/\/$/, '');

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');

  const token = tokenStore.get();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers });
  if (response.status === 401) {
    tokenStore.clear();
  }
  return response;
}

export async function apiJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await apiFetch(path, init);
  const body = (await response.json().catch(() => ({}))) as { detail?: string } & T;
  if (!response.ok) {
    throw new ApiError(response.status, body.detail || `API request failed (${response.status})`);
  }
  return body;
}
