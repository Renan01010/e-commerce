import axios from 'axios';
import type { UserProfile } from '../types/auth';
import { apiClient } from './apiClient';

export class AccountServiceError extends Error {
  constructor(readonly kind: 'unauthenticated' | 'invalid-input' | 'unavailable', message: string) {
    super(message);
    this.name = 'AccountServiceError';
  }
}

function mapAccountError(error: unknown): AccountServiceError {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 401) {
      return new AccountServiceError('unauthenticated', 'Sua sessão expirou. Entre novamente para continuar.');
    }
    if (error.response?.status === 400) {
      return new AccountServiceError('invalid-input', 'Confira os dados informados e tente novamente.');
    }
  }
  return new AccountServiceError('unavailable', 'Não foi possível acessar sua conta. Tente novamente.');
}

function isUserProfile(value: unknown): value is UserProfile {
  if (!value || typeof value !== 'object') return false;
  const profile = value as Partial<UserProfile>;
  return typeof profile.id === 'string'
    && typeof profile.email === 'string'
    && typeof profile.emailVerified === 'boolean'
    && (typeof profile.name === 'string' || profile.name === null || profile.name === undefined);
}

async function profileRequest(request: () => Promise<{ data: unknown }>): Promise<UserProfile> {
  try {
    const { data } = await request();
    if (!isUserProfile(data)) throw new AccountServiceError('unavailable', 'Não foi possível carregar os dados da conta.');
    return { ...data, name: data.name ?? null };
  } catch (error) {
    if (error instanceof AccountServiceError) throw error;
    throw mapAccountError(error);
  }
}

export const accountService = {
  getProfile(): Promise<UserProfile> {
    return profileRequest(() => apiClient.get('/users/me'));
  },

  updateName(name: string): Promise<UserProfile> {
    return profileRequest(() => apiClient.put('/users/me', { name: name.trim() }));
  },

  async changePassword(currentPassword: string, newPassword: string, confirmPassword: string): Promise<void> {
    try {
      await apiClient.put('/users/me/password', { currentPassword, newPassword, confirmPassword });
    } catch (error) {
      throw mapAccountError(error);
    }
  },
};
