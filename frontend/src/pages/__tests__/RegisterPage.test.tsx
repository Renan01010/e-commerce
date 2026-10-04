import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthServiceError, authService } from '../../services/authService';
import { RegisterPage } from '../RegisterPage';

vi.mock('../../services/authService', () => ({
  authService: { register: vi.fn() },
  AuthServiceError: class extends Error {
    constructor(readonly kind: string, message: string) {
      super(message);
    }
  },
}));

const createdAccount = {
  id: 'account-id', name: 'Ana Lima', email: 'ana@example.com', emailVerified: false as const,
  role: 'USER', active: true, createdAt: '2026-01-01', updatedAt: '2026-01-01',
};

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.mocked(authService.register).mockReset();
  });

  it('validates matching password confirmation before sending', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    await user.type(screen.getByLabelText('Nome'), 'Ana Lima');
    await user.type(screen.getByLabelText('E-mail'), 'ana@example.com');
    await user.type(screen.getByLabelText('Senha'), 'secure-pass');
    await user.type(screen.getByLabelText('Confirmar senha'), 'different-pass');
    await user.click(screen.getByRole('button', { name: 'Criar conta' }));

    expect(screen.getByRole('alert')).toHaveTextContent('As senhas não coincidem.');
    expect(authService.register).not.toHaveBeenCalled();
  });

  it('shows a safe resend instruction without claiming delivery after account creation', async () => {
    const user = userEvent.setup();
    vi.mocked(authService.register).mockResolvedValue({ ...createdAccount, emailSent: false });
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    await user.type(screen.getByLabelText('Nome'), 'Ana Lima');
    await user.type(screen.getByLabelText('E-mail'), 'ana@example.com');
    await user.type(screen.getByLabelText('Senha'), 'secure-pass');
    await user.type(screen.getByLabelText('Confirmar senha'), 'secure-pass');
    await user.click(screen.getByRole('button', { name: 'Criar conta' }));

    expect(await screen.findByRole('status')).toHaveTextContent('não foi possível enviar');
    expect(screen.getByRole('link', { name: 'Solicitar confirmação' })).toHaveAttribute('href', '/verify-email/resend');
    expect(authService.register).toHaveBeenCalledWith({
      name: 'Ana Lima',
      email: 'ana@example.com',
      password: 'secure-pass',
      confirmPassword: 'secure-pass',
    });
  });

  it('displays duplicate email errors returned by the service', async () => {
    const user = userEvent.setup();
    vi.mocked(authService.register).mockRejectedValue(
      new AuthServiceError('duplicate', 'Este e-mail já possui uma conta.'),
    );
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    await user.type(screen.getByLabelText('Nome'), 'Ana Lima');
    await user.type(screen.getByLabelText('E-mail'), 'ana@example.com');
    await user.type(screen.getByLabelText('Senha'), 'secure-pass');
    await user.type(screen.getByLabelText('Confirmar senha'), 'secure-pass');
    await user.click(screen.getByRole('button', { name: 'Criar conta' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Este e-mail já possui uma conta.');
  });
});
