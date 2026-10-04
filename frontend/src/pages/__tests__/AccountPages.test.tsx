import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { accountService } from '../../services/accountService';
import { useAuthStore } from '../../store/authStore';
import type { UserProfile } from '../../types/auth';
import { AccountPage } from '../AccountPage';
import { AccountSecurityPage } from '../AccountSecurityPage';

vi.mock('../../services/accountService', () => ({
  accountService: {
    getProfile: vi.fn(),
    updateName: vi.fn(),
    changePassword: vi.fn(),
  },
  AccountServiceError: class extends Error {},
}));

const profile: UserProfile = {
  id: 'account-id', name: 'Ana Lima', email: 'ana@example.com', emailVerified: true,
};

describe('account pages', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    useAuthStore.getState().setSession({
      accessToken: 'account-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000,
    });
    vi.mocked(accountService.getProfile).mockReset().mockResolvedValue(profile);
    vi.mocked(accountService.updateName).mockReset().mockImplementation(async (name) => ({ ...profile, name }));
    vi.mocked(accountService.changePassword).mockReset().mockResolvedValue();
  });

  it('shows the allowed profile fields and updates only the name', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><AccountPage /></MemoryRouter>);

    expect(await screen.findByLabelText('E-mail')).toHaveValue('ana@example.com');
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('readonly');
    await user.clear(screen.getByLabelText('Nome'));
    await user.type(screen.getByLabelText('Nome'), 'Ana Nova');
    await user.click(screen.getByRole('button', { name: 'Salvar nome' }));

    expect(accountService.updateName).toHaveBeenCalledWith('Ana Nova');
    expect(await screen.findByRole('status')).toHaveTextContent('Nome atualizado.');
  });

  it('requires matching new passwords and clears the local session after a successful change', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/account/security']}>
        <Routes>
          <Route path="/account/security" element={<AccountSecurityPage />} />
          <Route path="/login" element={<h1>Login</h1>} />
        </Routes>
      </MemoryRouter>,
    );
    await user.type(screen.getByLabelText('Senha atual'), 'old-password');
    await user.type(screen.getByLabelText('Nova senha'), 'new-password');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'different-password');
    await user.click(screen.getByRole('button', { name: 'Alterar senha' }));

    expect(screen.getByRole('alert')).toHaveTextContent('As senhas não coincidem.');
    expect(accountService.changePassword).not.toHaveBeenCalled();

    await user.clear(screen.getByLabelText('Confirmar nova senha'));
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'new-password');
    await user.click(screen.getByRole('button', { name: 'Alterar senha' }));

    expect(await screen.findByRole('heading', { name: 'Login' })).toBeInTheDocument();
    expect(accountService.changePassword).toHaveBeenCalledWith('old-password', 'new-password', 'new-password');
    expect(useAuthStore.getState().session).toBeNull();
  });
});
