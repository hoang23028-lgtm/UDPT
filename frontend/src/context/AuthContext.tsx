import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { AuthResponse, User } from '../api/types';
import { apiFetch, clearToken, getToken, setToken } from '../api/client';

type AuthContextValue = {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string, role: 'STUDENT' | 'TEACHER') => Promise<void>;
  logout: () => void;
  refreshMe: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const applyAuth = useCallback((data: AuthResponse) => {
    setToken(data.accessToken);
    setUser(data.user);
  }, []);

  const refreshMe = useCallback(async () => {
    const token = getToken();
    if (!token) {
      setUser(null);
      return;
    }
    try {
      const me = await apiFetch<User>('/api/users/me');
      setUser(me);
    } catch {
      clearToken();
      setUser(null);
    }
  }, []);

  useEffect(() => {
    void (async () => {
      await refreshMe();
      setLoading(false);
    })();
  }, [refreshMe]);

  useEffect(() => {
    const onAuthChanged = () => {
      void refreshMe();
    };
    window.addEventListener('udpt:auth-changed', onAuthChanged);
    window.addEventListener('storage', onAuthChanged);
    return () => {
      window.removeEventListener('udpt:auth-changed', onAuthChanged);
      window.removeEventListener('storage', onAuthChanged);
    };
  }, [refreshMe]);

  const login = useCallback(
    async (email: string, password: string) => {
      const body = JSON.stringify({ email, password });
      const data = await apiFetch<AuthResponse>('/api/auth/login', { method: 'POST', body });
      applyAuth(data);
    },
    [applyAuth],
  );

  const register = useCallback(
    async (email: string, password: string, displayName: string, role: 'STUDENT' | 'TEACHER') => {
      const body = JSON.stringify({ email, password, displayName, role });
      const data = await apiFetch<AuthResponse>('/api/auth/register', { method: 'POST', body });
      applyAuth(data);
    },
    [applyAuth],
  );

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, register, logout, refreshMe }),
    [user, loading, login, register, logout, refreshMe],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}
