import { useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import type { ReactNode } from 'react';

export function RequireAuth({ children }: { children: ReactNode }) {
  const location = useLocation();
  const session = useAuthStore((state) => state.session);
  const clearSession = useAuthStore((state) => state.clearSession);
  const isAuthenticated = Boolean(session && session.expiresAt > Date.now());

  useEffect(() => {
    if (session && !isAuthenticated) clearSession();
  }, [clearSession, isAuthenticated, session]);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}${location.hash}` }} />;
  }
  return children;
}
