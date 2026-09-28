import { afterEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../authStore';

describe('authStore', () => {
  afterEach(() => {
    useAuthStore.getState().clearSession();
    vi.restoreAllMocks();
  });

  it('starts unauthenticated and clears an established in-memory session', () => {
    expect(useAuthStore.getState().session).toBeNull();

    useAuthStore.getState().setSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 60_000,
    });
    expect(useAuthStore.getState().session?.accessToken).toBe('access-token');

    useAuthStore.getState().clearSession();
    expect(useAuthStore.getState().session).toBeNull();
  });

  it('does not expose an expired session as valid', () => {
    useAuthStore.getState().setSession({
      accessToken: 'expired-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() - 1,
    });

    expect(useAuthStore.getState().getValidSession()).toBeNull();
    expect(useAuthStore.getState().session).toBeNull();
  });

  it('keeps state in memory without registering storage persistence', () => {
    const storageSpy = vi.spyOn(Storage.prototype, 'setItem');

    useAuthStore.getState().setSession({
      accessToken: 'memory-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 60_000,
    });

    expect(storageSpy).not.toHaveBeenCalled();
  });
});