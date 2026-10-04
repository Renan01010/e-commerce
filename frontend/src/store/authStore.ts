import { create } from 'zustand';
import type { AuthSession, UserProfile } from '../types/auth';

interface AuthState {
  session: AuthSession | null;
  profile: UserProfile | null;
  setSession: (session: AuthSession) => void;
  setProfile: (profile: UserProfile) => void;
  clearSession: () => void;
  getValidSession: () => AuthSession | null;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  session: null,
  profile: null,
  setSession: (session) => set({ session }),
  setProfile: (profile) => set({ profile }),
  clearSession: () => set({ session: null, profile: null }),
  getValidSession: () => {
    const session = get().session;
    if (session && session.expiresAt > Date.now()) return session;
    if (session) set({ session: null, profile: null });
    return null;
  },
}));