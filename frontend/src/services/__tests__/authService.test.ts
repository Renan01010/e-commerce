import axios from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from '../apiClient';
import { AuthServiceError, authService } from '../authService';

vi.mock('../apiClient', () => ({
  apiClient: { post: vi.fn() },
}));

describe('authService', () => {
  afterEach(() => vi.resetAllMocks());

  it('posts the supplied credentials to the existing login route', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      data: { accessToken: 'access-token', tokenType: 'Bearer', expiresIn: 86400 },
    });

    await expect(authService.login({ email: 'client@example.com', password: 'secret' }))
      .resolves.toEqual({ accessToken: 'access-token', tokenType: 'Bearer', expiresIn: 86400 });
    expect(apiClient.post).toHaveBeenCalledWith('/auth/login', {
      email: 'client@example.com',
      password: 'secret',
    });
  });

  it('rejects a malformed success response', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { tokenType: 'Bearer', expiresIn: 86400 } });

    await expect(authService.login({ email: 'client@example.com', password: 'secret' }))
      .rejects.toMatchObject({ kind: 'invalid-response' });
  });

  it('maps invalid credentials without exposing backend details', async () => {
    vi.mocked(apiClient.post).mockRejectedValue({
      isAxiosError: true,
      response: { status: 401, data: { message: 'private auth diagnostic' } },
    });

    await expect(authService.login({ email: 'client@example.com', password: 'secret' }))
      .rejects.toMatchObject({ kind: 'credentials' });
    await expect(authService.login({ email: 'client@example.com', password: 'secret' }))
      .rejects.not.toThrow('private auth diagnostic');
  });

  it('maps network failures to a retryable unavailable error', async () => {
    vi.mocked(apiClient.post).mockRejectedValue(new axios.AxiosError('offline'));

    await expect(authService.login({ email: 'client@example.com', password: 'secret' }))
      .rejects.toMatchObject({ kind: 'unavailable' });
  });

  it('exposes a safe typed error message', () => {
    const error = new AuthServiceError('credentials', 'E-mail ou senha inválidos.');
    expect(error.message).toBe('E-mail ou senha inválidos.');
  });

  it('registers with a normalized email and does not accept a malformed account response', async () => {
    const account = {
      id: 'account-id', name: 'Ana Lima', email: 'ana@example.com', emailVerified: false as const,
      role: 'USER', active: true, createdAt: '2026-01-01', updatedAt: '2026-01-01',
    };
    vi.mocked(apiClient.post).mockResolvedValue({ data: account });

    await expect(authService.register({
      name: ' Ana Lima ', email: ' ANA@EXAMPLE.COM ', password: 'secure-pass', confirmPassword: 'secure-pass',
    })).resolves.toEqual(account);
    expect(apiClient.post).toHaveBeenCalledWith('/auth/register', {
      name: 'Ana Lima', email: 'ana@example.com', password: 'secure-pass', confirmPassword: 'secure-pass',
    });

    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { emailVerified: true } });
    await expect(authService.register({
      name: 'Ana', email: 'ana@example.com', password: 'secure-pass', confirmPassword: 'secure-pass',
    }))
      .rejects.toMatchObject({ kind: 'invalid-response' });
  });

  it('uses the public verification and password-recovery contract routes', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { message: 'accepted' } });

    await authService.verifyEmail('verify-token');
    await authService.resendVerification(' ANA@EXAMPLE.COM ');
    await authService.requestPasswordReset(' ANA@EXAMPLE.COM ');
    await authService.resetPassword('reset-token', 'new-password', 'new-password');

    expect(apiClient.post).toHaveBeenNthCalledWith(1, '/auth/verify-email', { token: 'verify-token' });
    expect(apiClient.post).toHaveBeenNthCalledWith(2, '/auth/resend-verification', { email: 'ana@example.com' });
    expect(apiClient.post).toHaveBeenNthCalledWith(3, '/auth/forgot-password', { email: 'ana@example.com' });
    expect(apiClient.post).toHaveBeenNthCalledWith(4, '/auth/reset-password', {
      token: 'reset-token', newPassword: 'new-password', confirmPassword: 'new-password',
    });
  });
});