import axios from 'axios';
import type {
  AccountCreatedResponse,
  LoginCredentials,
  LoginResponse,
  RegisterRequest,
} from '../types/auth';
import { apiClient } from './apiClient';

export type AuthServiceErrorKind =
  | 'credentials'
  | 'duplicate'
  | 'invalid-input'
  | 'invalid-token'
  | 'unavailable'
  | 'invalid-response';

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

function isAccountCreatedResponse(value: unknown): value is AccountCreatedResponse {
  if (!value || typeof value !== 'object') return false;
  const response = value as Partial<AccountCreatedResponse>;
  return typeof response.id === 'string'
    && typeof response.email === 'string'
    && response.emailVerified === false
    && typeof response.role === 'string'
    && typeof response.active === 'boolean'
    && typeof response.createdAt === 'string'
    && typeof response.updatedAt === 'string';
}

function mapError(error: unknown, operation: 'login' | 'register' | 'token' | 'generic'): AuthServiceError {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    if (operation === 'login' && status === 401) {
      return new AuthServiceError('credentials', 'E-mail ou senha inválidos.');
    }
    if (operation === 'register' && status === 409) {
      return new AuthServiceError('duplicate', 'Este e-mail já possui uma conta.');
    }
    if (status === 400) {
      if (operation === 'token') {
        return new AuthServiceError('invalid-token', 'Este link é inválido ou expirou. Solicite um novo link para continuar.');
      }
      return new AuthServiceError('invalid-input', 'Confira os dados informados e tente novamente.');
    }
  }
  return new AuthServiceError('unavailable', 'Não foi possível concluir a solicitação. Tente novamente.');
}

async function runRequest<T>(operation: 'register' | 'token' | 'generic', request: () => Promise<T>): Promise<T> {
  try {
    return await request();
  } catch (error) {
    if (error instanceof AuthServiceError) throw error;
    throw mapError(error, operation);
  }
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
      throw mapError(error, 'login');
    }
  },

  async register(request: RegisterRequest): Promise<AccountCreatedResponse> {
    return runRequest('register', async () => {
      const { data } = await apiClient.post<unknown>('/auth/register', {
        name: request.name.trim(),
        email: request.email.trim().toLowerCase(),
        password: request.password,
        confirmPassword: request.confirmPassword,
      });
      if (!isAccountCreatedResponse(data)) {
        throw new AuthServiceError('invalid-response', 'A conta não pôde ser confirmada. Tente novamente.');
      }
      return data;
    });
  },

  async verifyEmail(token: string): Promise<void> {
    await runRequest('token', () => apiClient.post('/auth/verify-email', { token }).then(() => undefined));
  },

  async resendVerification(email: string): Promise<void> {
    await runRequest('generic', () => apiClient.post('/auth/resend-verification', { email: email.trim().toLowerCase() })
      .then(() => undefined));
  },

  async requestPasswordReset(email: string): Promise<void> {
    await runRequest('generic', () => apiClient.post('/auth/forgot-password', { email: email.trim().toLowerCase() })
      .then(() => undefined));
  },

  async resetPassword(token: string, newPassword: string, confirmPassword: string): Promise<void> {
    await runRequest('token', () => apiClient.post('/auth/reset-password', {
      token, newPassword, confirmPassword,
    }).then(() => undefined));
  },
};
