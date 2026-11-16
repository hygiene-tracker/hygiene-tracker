const TOKEN_KEY = 'hygiene_tracker_app_jwt';

export const tokenStore = {
  get(): string | null {
    if (typeof window === 'undefined') return null;
    try {
      return window.localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },

  set(token: string): void {
    if (typeof window === 'undefined') return;
    try {
      window.localStorage.setItem(TOKEN_KEY, token);
    } catch {
      // The UI can still show the sign-in result if storage is unavailable.
    }
  },

  clear(): void {
    if (typeof window === 'undefined') return;
    try {
      window.localStorage.removeItem(TOKEN_KEY);
    } catch {
      // Ignore storage failures during logout or a 401 response.
    }
  },
};
