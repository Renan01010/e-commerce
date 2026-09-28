import axios from 'axios';
import type { LoginCredentials, LoginResponse } from '../types/auth';
import { apiClient } from './apiClient';

export type AuthServiceErrorKind = 'credentials' | 'unavailable' | 'invalid-response';

export class AuthServiceError extends Error {
  constructor(readonly kind: AuthServiceErrorKind, message: string) {
    super(message);
    this.name = 'AuthServiceError';
  }
}

function isLoginResponse(value: unknown): value is LoginResponse {
  if (!value || typeof value !== 'object') return false;
  const response = value as Partial<LoginResponse>;
  return typeof response.accessToken === 'string'
    && response.accessToken.trim().length > 0
    && response.tokenType === 'Bearer'
    && typeof response.expiresIn === 'number'
    && Number.isInteger(response.expiresIn)
    && response.expiresIn > 0;
}

export const authService = {
  async login(credentials: LoginCredentials): Promise<LoginResponse> {
    try {
      const { data } = await apiClient.post<unknown>('/auth/login', {
        email: credentials.email.trim(),
        password: credentials.password,
      });
      if (!isLoginResponse(data)) {
        throw new AuthServiceError('invalid-response', 'Não foi possível concluir o login. Tente novamente.');
      }
      return data;
    } catch (error) {
      if (error instanceof AuthServiceError) throw error;
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        throw new AuthServiceError('credentials', 'E-mail ou senha inválidos.');
      }
      throw new AuthServiceError('unavailable', 'Não foi possível conectar. Tente novamente.');
    }
  },
};