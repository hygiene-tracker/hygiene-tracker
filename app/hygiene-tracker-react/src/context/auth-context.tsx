import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';

import {
  clearAuth,
  loadAuth,
  saveAuth,
  type AuthResponse,
  type AuthUser,
} from '@/services/auth';

type AuthContextValue = {
  user: AuthUser | null;
  token: string | null;
  isLoading: boolean;
  signIn: (response: AuthResponse) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [auth, setAuth] = useState<AuthResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    void loadAuth()
      .then((storedAuth) => {
        if (active) setAuth(storedAuth);
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: auth?.user ?? null,
      token: auth?.token ?? null,
      isLoading,
      signIn: async (response) => {
        await saveAuth(response);
        setAuth(response);
      },
      signOut: async () => {
        await clearAuth();
        setAuth(null);
      },
    }),
    [auth, isLoading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
