import { create } from 'zustand';
import type { AuthSession } from '../types/auth';

interface AuthState {
  session: AuthSession | null;
  setSession: (session: AuthSession) => void;
  clearSession: () => void;
  getValidSession: () => AuthSession | null;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  session: null,
  setSession: (session) => set({ session }),
  clearSession: () => set({ session: null }),
  getValidSession: () => {
    const session = get().session;
    if (session && session.expiresAt > Date.now()) return session;
    if (session) set({ session: null });
    return null;
  },
}));