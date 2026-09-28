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