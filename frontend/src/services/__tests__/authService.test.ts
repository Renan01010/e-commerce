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
});