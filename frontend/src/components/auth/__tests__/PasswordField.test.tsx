import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PasswordField } from '../PasswordField';

describe('PasswordField', () => {
  it('starts hidden and toggles visibility without changing the value', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<PasswordField id="password" value="secret" onChange={onChange} disabled={false} />);

    const input = screen.getByLabelText('Senha');
    expect(input).toHaveAttribute('type', 'password');

    const showButton = screen.getByRole('button', { name: 'Mostrar senha' });
    showButton.focus();
    await user.keyboard('{Enter}');
    expect(input).toHaveAttribute('type', 'text');
    expect(input).toHaveValue('secret');
    expect(screen.getByRole('button', { name: 'Ocultar senha' })).toHaveAttribute('aria-pressed', 'true');

    await user.click(screen.getByRole('button', { name: 'Ocultar senha' }));
    expect(input).toHaveAttribute('type', 'password');
    expect(input).toHaveValue('secret');
  });
});