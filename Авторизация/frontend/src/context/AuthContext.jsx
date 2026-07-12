import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { apiFetch, clearCsrf, refreshCsrf, setUnauthorizedHandler } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState('loading');

  const becomeGuest = useCallback(() => {
    setUser(null);
    setStatus('guest');
  }, []);

  const refreshCurrentUser = useCallback(async () => {
    try {
      const currentUser = await apiFetch('/api/auth/me', { skipUnauthorizedHandler: true });
      setUser(currentUser);
      setStatus('authenticated');
      return currentUser;
    } catch (error) {
      if (error?.status === 401) becomeGuest();
      throw error;
    }
  }, [becomeGuest]);

  useEffect(() => {
    setUnauthorizedHandler(async ({ status, code }) => {
      if (status === 401) {
        becomeGuest();
      } else if (status === 403 && code === 'ACCESS_DENIED') {
        await refreshCurrentUser().catch(() => {});
      }
    });
    return () => setUnauthorizedHandler(null);
  }, [becomeGuest, refreshCurrentUser]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        await refreshCsrf();
        const me = await apiFetch('/api/auth/me', { skipUnauthorizedHandler: true });
        if (active) { setUser(me); setStatus('authenticated'); }
      } catch (error) {
        if (active && error?.status === 401) becomeGuest();
        else if (active) setStatus('guest');
      }
    })();
    return () => { active = false; };
  }, [becomeGuest]);

  const login = useCallback(async (credentials) => {
    const loggedIn = await apiFetch('/api/auth/login', { method: 'POST', body: credentials });
    clearCsrf();
    await refreshCsrf();
    setUser(loggedIn);
    setStatus('authenticated');
    return loggedIn;
  }, []);

  const register = useCallback((payload) => apiFetch('/api/auth/register', { method: 'POST', body: payload }), []);

  const logout = useCallback(async () => {
    await apiFetch('/api/auth/logout', { method: 'POST' });
    becomeGuest();
    clearCsrf();
    await refreshCsrf().catch(() => {});
  }, [becomeGuest]);

  const value = useMemo(() => ({
    user,
    status,
    isAuthenticated: status === 'authenticated',
    isAdmin: user?.role === 'ADMIN',
    login,
    register,
    logout,
  }), [user, status, login, register, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth должен использоваться внутри AuthProvider');
  return value;
}
