import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import type { LoginCredentials, LoginResponse } from '../../types/auth';
import { authService } from '../../services/authService';
import { useAuthStore } from '../../store/authStore';
import { LoginPage } from '../LoginPage';

vi.mock('../../services/authService', () => ({
  authService: { login: vi.fn() },
}));

describe('LoginPage', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    vi.mocked(authService.login).mockReset();
  });

  it('stores a valid token in memory and redirects to the home route', async () => {
    const user = userEvent.setup();
    const response: LoginResponse = {
      accessToken: 'valid-access-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    };
    vi.mocked(authService.login).mockResolvedValue(response);

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<h1>TechStore Home</h1>} />
        </Routes>
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('E-mail'), 'client@example.com');
    await user.type(screen.getByLabelText('Senha'), 'correct-password');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('heading', { name: 'TechStore Home' })).toBeInTheDocument();
    expect(authService.login).toHaveBeenCalledWith({
      email: 'client@example.com',
      password: 'correct-password',
    } satisfies LoginCredentials);
    expect(useAuthStore.getState().session).toEqual({
      accessToken: response.accessToken,
      tokenType: response.tokenType,
      expiresAt: expect.any(Number),
    });
  });

  it('keeps the login page standalone without the shared store header', () => {
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes><Route path="/login" element={<LoginPage />} /></Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole('main')).toHaveClass('login-screen');
    expect(screen.queryByRole('banner')).not.toBeInTheDocument();
  });

  it('returns to a safe protected destination provided by the route guard', async () => {
    const user = userEvent.setup();
    vi.mocked(authService.login).mockResolvedValue({
      accessToken: 'valid-access-token', tokenType: 'Bearer', expiresIn: 3600,
    });
    render(
      <MemoryRouter initialEntries={[{
        pathname: '/login',
        state: { from: '/account/security?tab=password' },
      }]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/account/security" element={<h1>Segurança da conta</h1>} />
          <Route path="/" element={<h1>TechStore Home</h1>} />
        </Routes>
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('E-mail'), 'client@example.com');
    await user.type(screen.getByLabelText('Senha'), 'correct-password');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('heading', { name: 'Segurança da conta' })).toBeInTheDocument();
  });
});