import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { CreateAccountLink } from '../CreateAccountLink';
import { RegisterUnavailablePage } from '../../../pages/RegisterUnavailablePage';

describe('CreateAccountLink', () => {
  it('navigates by keyboard to the non-functional registration placeholder', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<CreateAccountLink />} />
          <Route path="/register" element={<RegisterUnavailablePage />} />
        </Routes>
      </MemoryRouter>,
    );

    const link = screen.getByRole('link', { name: 'Criar conta' });
    link.focus();
    await user.keyboard('{Enter}');

    expect(await screen.findByRole('heading', { name: 'Cadastro indisponível no momento' })).toBeInTheDocument();
  });
});