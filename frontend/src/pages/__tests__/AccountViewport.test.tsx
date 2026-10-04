import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { accountService } from '../../services/accountService';
import { authService } from '../../services/authService';
import { useAuthStore } from '../../store/authStore';
import type { UserProfile } from '../../types/auth';
import { AccountPage } from '../AccountPage';
import { AccountSecurityPage } from '../AccountSecurityPage';
import { ForgotPasswordPage } from '../ForgotPasswordPage';
import { RegisterPage } from '../RegisterPage';
import { ResendVerificationPage } from '../ResendVerificationPage';
import { ResetPasswordPage } from '../ResetPasswordPage';
import { VerifyEmailPage } from '../VerifyEmailPage';

vi.mock('../../services/accountService', () => ({
  accountService: {
    getProfile: vi.fn(),
    updateName: vi.fn(),
    changePassword: vi.fn(),
  },
  AccountServiceError: class extends Error {},
}));

vi.mock('../../services/authService', () => ({
  authService: {
    register: vi.fn(),
    verifyEmail: vi.fn(),
    resendVerification: vi.fn(),
    requestPasswordReset: vi.fn(),
    resetPassword: vi.fn(),
  },
  AuthServiceError: class extends Error {},
}));

const profile: UserProfile = {
  id: 'viewport-account',
  name: 'Ana Lima',
  email: 'ana@example.com',
  emailVerified: true,
};

const originalViewportWidth = window.innerWidth;

// jsdom performs no layout, so these tests verify that each screen renders its content, controls,
// navigation and accessible names at both viewport widths; overflow/visual layout needs a real browser.
describe.each([375, 1280])('account screens at %ipx viewport width', (viewportWidth) => {
  afterEach(() => {
    Object.defineProperty(window, 'innerWidth', {
      configurable: true,
      writable: true,
      value: originalViewportWidth,
    });
  });

  beforeEach(() => {
    Object.defineProperty(window, 'innerWidth', {
      configurable: true,
      writable: true,
      value: viewportWidth,
    });
    window.dispatchEvent(new Event('resize'));
    useAuthStore.getState().clearSession();
    useAuthStore.getState().setSession({
      accessToken: 'viewport-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 60_000,
    });
    vi.mocked(accountService.getProfile).mockResolvedValue(profile);
    vi.mocked(authService.verifyEmail).mockReturnValue(new Promise(() => undefined));
  });

  function expectLandmarks(heading: string) {
    expect(window.innerWidth).toBe(viewportWidth);
    expect(screen.getByRole('main')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: heading })).toBeInTheDocument();
  }

  it('renders registration with labelled fields, submit action and login navigation', () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);

    expectLandmarks('Crie sua conta');
    for (const label of ['Nome', 'E-mail', 'Senha', 'Confirmar senha']) {
      expect(screen.getByLabelText(label)).toBeEnabled();
    }
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('type', 'email');
    expect(screen.getByLabelText('Senha')).toHaveAttribute('type', 'password');
    expect(screen.getByRole('button', { name: 'Criar conta' })).toBeEnabled();
    expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login');
  });

  it('renders email verification progress with login navigation', () => {
    render(<MemoryRouter initialEntries={['/verify-email?token=viewport']}><VerifyEmailPage /></MemoryRouter>);

    expectLandmarks('Confirmando seu e-mail');
    expect(screen.getByRole('status')).toHaveTextContent('Validando link...');
    expect(screen.getByRole('link', { name: 'Ir para o login' })).toHaveAttribute('href', '/login');
  });

  it('renders the incomplete verification link state with a resend action', () => {
    render(<MemoryRouter initialEntries={['/verify-email']}><VerifyEmailPage /></MemoryRouter>);

    expect(screen.getByRole('alert')).toHaveTextContent('O link está incompleto');
    expect(screen.getByRole('link', { name: 'Solicitar novo link' })).toHaveAttribute('href', '/verify-email/resend');
  });

  it('renders resend verification with its form and login navigation', () => {
    render(<MemoryRouter><ResendVerificationPage /></MemoryRouter>);

    expectLandmarks('Solicitar novo link');
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('type', 'email');
    expect(screen.getByRole('button', { name: 'Solicitar link' })).toBeEnabled();
    expect(screen.getByRole('link', { name: 'Voltar ao login' })).toHaveAttribute('href', '/login');
  });

  it('renders forgot password with its form and login navigation', () => {
    render(<MemoryRouter><ForgotPasswordPage /></MemoryRouter>);

    expectLandmarks('Esqueceu sua senha?');
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('type', 'email');
    expect(screen.getByRole('button', { name: 'Solicitar redefinição' })).toBeEnabled();
    expect(screen.getByRole('link', { name: 'Voltar ao login' })).toHaveAttribute('href', '/login');
  });

  it('renders reset password with both password fields and recovery navigation', () => {
    render(<MemoryRouter initialEntries={['/reset-password?token=viewport']}><ResetPasswordPage /></MemoryRouter>);

    expectLandmarks('Defina uma nova senha');
    expect(screen.getByLabelText('Nova senha')).toHaveAttribute('type', 'password');
    expect(screen.getByLabelText('Confirmar nova senha')).toHaveAttribute('type', 'password');
    expect(screen.getByRole('button', { name: 'Redefinir senha' })).toBeEnabled();
    expect(screen.getByRole('link', { name: 'Solicitar outro link' })).toHaveAttribute('href', '/forgot-password');
  });

  it('renders profile controls and navigation to security', async () => {
    render(<MemoryRouter><AccountPage /></MemoryRouter>);

    expect(await screen.findByLabelText('Nome')).toHaveValue('Ana Lima');
    expectLandmarks('Minha conta');
    expect(screen.getByLabelText('E-mail')).toHaveValue('ana@example.com');
    expect(screen.getByRole('button', { name: 'Salvar nome' })).toBeEnabled();
    expect(screen.getByRole('navigation', { name: 'Seções da conta' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Segurança' })).toHaveAttribute('href', '/account/security');
  });

  it('renders password security controls and navigation back to the profile', () => {
    render(<MemoryRouter><AccountSecurityPage /></MemoryRouter>);

    expectLandmarks('Segurança');
    expect(screen.getByLabelText('Senha atual')).toBeEnabled();
    expect(screen.getByLabelText('Nova senha')).toBeEnabled();
    expect(screen.getByLabelText('Confirmar nova senha')).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Alterar senha' })).toBeEnabled();
    expect(screen.getByRole('link', { name: 'Perfil' })).toHaveAttribute('href', '/account');
  });
});
