import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthServiceError, authService } from '../../services/authService';
import { useAuthStore } from '../../store/authStore';
import { ForgotPasswordPage } from '../ForgotPasswordPage';
import { ResendVerificationPage } from '../ResendVerificationPage';
import { ResetPasswordPage } from '../ResetPasswordPage';
import { VerifyEmailPage } from '../VerifyEmailPage';

vi.mock('../../services/authService', () => ({
  authService: {
    verifyEmail: vi.fn(),
    resendVerification: vi.fn(),
    requestPasswordReset: vi.fn(),
    resetPassword: vi.fn(),
  },
  AuthServiceError: class extends Error {
    constructor(readonly kind: string, message: string) {
      super(message);
    }
  },
}));

describe('account recovery pages', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('verifies an email token from the URL and offers safe recovery for invalid tokens', async () => {
    vi.mocked(authService.verifyEmail).mockRejectedValue(new AuthServiceError('invalid-token', 'Este link expirou.'));
    render(<MemoryRouter initialEntries={['/verify-email?token=verify-token']}><VerifyEmailPage /></MemoryRouter>);

    expect(await screen.findByRole('alert')).toHaveTextContent('Este link expirou.');
    expect(authService.verifyEmail).toHaveBeenCalledWith('verify-token');
    expect(screen.getByRole('link', { name: 'Solicitar novo link' })).toHaveAttribute('href', '/verify-email/resend');
  });

  it('uses an enumeration-safe acknowledgement to resend verification', async () => {
    const user = userEvent.setup();
    vi.mocked(authService.resendVerification).mockResolvedValue();
    render(<MemoryRouter><ResendVerificationPage /></MemoryRouter>);
    await user.type(screen.getByLabelText('E-mail'), ' ANA@EXAMPLE.COM ');
    await user.click(screen.getByRole('button', { name: 'Solicitar link' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Se houver uma confirmação pendente');
    expect(authService.resendVerification).toHaveBeenCalledWith('ANA@EXAMPLE.COM');
  });

  it('shows an enumeration-safe acknowledgement for password recovery', async () => {
    const user = userEvent.setup();
    vi.mocked(authService.requestPasswordReset).mockResolvedValue();
    render(<MemoryRouter><ForgotPasswordPage /></MemoryRouter>);
    await user.type(screen.getByLabelText('E-mail'), 'member@example.com');
    await user.click(screen.getByRole('button', { name: 'Solicitar redefinição' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Se houver uma conta associada');
    expect(authService.requestPasswordReset).toHaveBeenCalledWith('member@example.com');
  });

  it('requires confirmation and clears the in-memory session after resetting a password', async () => {
    const user = userEvent.setup();
    useAuthStore.getState().setSession({
      accessToken: 'old-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000,
    });
    vi.mocked(authService.resetPassword).mockResolvedValue();
    render(<MemoryRouter initialEntries={['/reset-password?token=reset-token']}><ResetPasswordPage /></MemoryRouter>);
    await user.type(screen.getByLabelText('Nova senha'), 'new-password');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'different-password');
    await user.click(screen.getByRole('button', { name: 'Redefinir senha' }));

    expect(screen.getByRole('alert')).toHaveTextContent('As senhas não coincidem.');
    expect(authService.resetPassword).not.toHaveBeenCalled();
    await user.clear(screen.getByLabelText('Confirmar nova senha'));
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'new-password');
    await user.click(screen.getByRole('button', { name: 'Redefinir senha' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Senha redefinida.');
    expect(authService.resetPassword).toHaveBeenCalledWith('reset-token', 'new-password', 'new-password');
    expect(useAuthStore.getState().session).toBeNull();
  });
});
