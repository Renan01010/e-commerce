import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from '../apiClient';
import { accountService } from '../accountService';

vi.mock('../apiClient', () => ({
  apiClient: { get: vi.fn(), put: vi.fn() },
}));

describe('accountService', () => {
  afterEach(() => vi.resetAllMocks());

  it('sends both password values and the confirmation to the authenticated password endpoint', async () => {
    vi.mocked(apiClient.put).mockResolvedValue({ data: undefined });

    await accountService.changePassword('current-password', 'new-password', 'new-password');

    expect(apiClient.put).toHaveBeenCalledWith('/users/me/password', {
      currentPassword: 'current-password',
      newPassword: 'new-password',
      confirmPassword: 'new-password',
    });
  });
});
