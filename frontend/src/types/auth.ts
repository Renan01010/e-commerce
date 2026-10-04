export interface LoginCredentials {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
}

export interface AuthSession {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: number;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface AccountCreatedResponse {
  id: string;
  name?: string | null;
  email: string;
  emailVerified: false;
  role: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  emailSent?: boolean;
  emailDeliveryStatus?: string;
}

export interface UserProfile {
  id: string;
  name: string | null;
  email: string;
  emailVerified: boolean;
  createdAt?: string;
}