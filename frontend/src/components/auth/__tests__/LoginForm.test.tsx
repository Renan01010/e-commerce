import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { LoginCredentials } from '../../../types/auth';
import { LoginForm } from '../LoginForm';

describe('LoginForm', () => {
  it('blocks empty required fields without calling submit', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<LoginForm onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(screen.getByText('Informe seu e-mail.')).toBeInTheDocument();
    expect(screen.getByText('Informe sua senha.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('blocks a malformed email before calling submit', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<LoginForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('E-mail'), 'not-an-email');
    await user.type(screen.getByLabelText('Senha'), 'secret');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(screen.getByText('Informe um e-mail válido.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('trims email and submits credentials once', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn<(credentials: LoginCredentials) => Promise<void>>().mockResolvedValue();
    render(<LoginForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('E-mail'), '  client@example.com  ');
    await user.type(screen.getByLabelText('Senha'), 'secret');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({ email: 'client@example.com', password: 'secret' });
  });

  it('disables controls and announces loading while submitting', () => {
    render(<LoginForm onSubmit={vi.fn()} isSubmitting />);

    expect(screen.getByRole('button', { name: /entrando/i })).toBeDisabled();
    expect(screen.getByLabelText('E-mail')).toBeDisabled();
    expect(screen.getByLabelText('Senha')).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent(/autenticando/i);
  });

  it('keeps form controls focusable in a predictable document order', () => {
    render(<LoginForm onSubmit={vi.fn()} />);
    const email = screen.getByLabelText('E-mail');
    const password = screen.getByLabelText('Senha');
    const visibility = screen.getByRole('button', { name: 'Mostrar senha' });
    const submit = screen.getByRole('button', { name: 'Entrar' });

    expect([email, password, visibility, submit].map((element) => element.tabIndex))
      .toEqual([0, 0, 0, 0]);
    for (const control of [email, password, visibility, submit]) {
      control.focus();
      expect(control).toHaveFocus();
    }
  });

  it('shows safe generic feedback supplied by the page', () => {
    render(<LoginForm onSubmit={vi.fn()} errorMessage="E-mail ou senha inválidos." />);

    expect(screen.getByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.');
  });
});