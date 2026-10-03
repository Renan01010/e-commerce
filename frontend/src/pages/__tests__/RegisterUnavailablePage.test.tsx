import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { RegisterUnavailablePage } from '../RegisterUnavailablePage';

describe('RegisterUnavailablePage', () => {
  it('shows the standalone unavailable state and links back to login', () => {
    render(
      <MemoryRouter initialEntries={['/register']}>
        <Routes>
          <Route path="/register" element={<RegisterUnavailablePage />} />
          <Route path="/login" element={<h1>Login</h1>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Cadastro indisponível no momento' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Voltar ao login' })).toHaveAttribute('href', '/login');
    expect(screen.queryByRole('banner')).not.toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  });
});
