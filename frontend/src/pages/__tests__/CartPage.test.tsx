import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cartApi } from '../../services/apiClient';
import { useAuthStore } from '../../store/authStore';
import { useCartStore } from '../../store/cartStore';
import type { CartItem, CartResponse } from '../../types/cart';
import { CartPage } from '../CartPage';

vi.mock('../../services/apiClient', () => ({
  catalogApi: { getProducts: vi.fn(), getCategories: vi.fn(), getProduct: vi.fn() },
  cartApi: { getCart: vi.fn(), addItem: vi.fn(), setQuantity: vi.fn(), removeItem: vi.fn(), clearCart: vi.fn() },
  getApiErrorMessage: () => 'Erro do catálogo',
}));

const availableItem: CartItem = {
  productId: 'product-1', quantity: 2, available: true,
  product: { name: 'Keyboard', price: 49.9, brand: 'Acme', imageUrl: null },
};
const unavailableItem: CartItem = { productId: 'product-2', quantity: 1, available: false, product: null };
const response = (...items: CartItem[]): CartResponse => ({ items });

function setAuthenticatedSession() {
  useAuthStore.getState().setSession({ accessToken: 'valid-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 });
}

function renderCart(initialEntry = '/cart') {
  return render(<MemoryRouter initialEntries={[initialEntry]}><Routes>
    <Route path="/cart" element={<CartPage />} />
    <Route path="/" element={<main>Catálogo</main>} />
    <Route path="/login" element={<main>Login</main>} />
  </Routes></MemoryRouter>);
}

describe('CartPage', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    useCartStore.setState({ items: [], status: 'idle', error: null, successMessage: null, pendingOperations: {} });
    vi.clearAllMocks();
    vi.mocked(cartApi.getCart).mockResolvedValue(response(availableItem, unavailableItem));
    vi.mocked(cartApi.setQuantity).mockResolvedValue(response({ ...availableItem, quantity: 3 }, unavailableItem));
    vi.mocked(cartApi.removeItem).mockResolvedValue(undefined);
    vi.mocked(cartApi.clearCart).mockResolvedValue(undefined);
  });

  it('loads available and unavailable rows and shows counts without a financial total', async () => {
    setAuthenticatedSession();
    renderCart();

    expect(await screen.findByRole('heading', { name: 'Seu carrinho' })).toBeInTheDocument();
    expect(screen.getByText('Keyboard')).toBeInTheDocument();
    expect(screen.getByText('Acme')).toBeInTheDocument();
    expect(screen.getByText('Produto indisponível')).toBeInTheDocument();
    expect(screen.getByText('product-2')).toBeInTheDocument();
    expect(screen.getByText('2 produtos')).toBeInTheDocument();
    expect(screen.getByText('3 unidades')).toBeInTheDocument();
    expect(screen.queryByText(/subtotal|frete|total geral/i)).not.toBeInTheDocument();
    expect(cartApi.getCart).toHaveBeenCalledTimes(1);
  });

  it('updates a quantity using the returned CartResponse', async () => {
    const user = userEvent.setup();
    setAuthenticatedSession();
    renderCart();

    await screen.findByText('Keyboard');
    await user.click(screen.getByRole('button', { name: 'Aumentar quantidade de Keyboard' }));

    expect(cartApi.setQuantity).toHaveBeenCalledWith('product-1', 3);
    expect(await screen.findByRole('spinbutton', { name: 'Quantidade de Keyboard' })).toHaveValue(3);
  });

  it('removes an unavailable product without sending quantity changes', async () => {
    const user = userEvent.setup();
    setAuthenticatedSession();
    renderCart();

    await screen.findByText('Produto indisponível');
    await user.click(screen.getByRole('button', { name: 'Remover Produto indisponível' }));

    expect(cartApi.removeItem).toHaveBeenCalledWith('product-2');
    expect(cartApi.setQuantity).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByText('product-2')).not.toBeInTheDocument());
  });

  it('requires clear confirmation and leaves the cart unchanged when cancelled', async () => {
    const user = userEvent.setup();
    setAuthenticatedSession();
    renderCart();
    await screen.findByText('Keyboard');

    await user.click(screen.getAllByRole('button', { name: 'Limpar carrinho' })[0]);
    const dialog = screen.getByRole('dialog', { name: 'Limpar carrinho?' });
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));
    expect(cartApi.clearCart).not.toHaveBeenCalled();
    expect(screen.getByText('Keyboard')).toBeInTheDocument();

    await user.click(screen.getAllByRole('button', { name: 'Limpar carrinho' })[0]);
    await user.click(within(screen.getByRole('dialog', { name: 'Limpar carrinho?' })).getByRole('button', { name: 'Confirmar limpeza' }));
    expect(cartApi.clearCart).toHaveBeenCalledTimes(1);
    expect(await screen.findByText('Seu carrinho está vazio.')).toBeInTheDocument();
  });

  it('keeps the route open and offers login without requesting a cart when unauthenticated', async () => {
    renderCart();

    expect(await screen.findByText('Entre na sua conta para ver seu carrinho.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Fazer login' })).toHaveAttribute('href', '/login');
    expect(cartApi.getCart).not.toHaveBeenCalled();
  });

  it('shows a loading state until the initial request resolves', async () => {
    let resolveCart: ((cart: CartResponse) => void) | undefined;
    vi.mocked(cartApi.getCart).mockReturnValue(new Promise((resolve) => { resolveCart = resolve; }));
    setAuthenticatedSession();
    renderCart();

    expect(screen.getByRole('status', { name: 'Carregando carrinho' })).toBeInTheDocument();
    resolveCart?.(response(availableItem));
    expect(await screen.findByText('Keyboard')).toBeInTheDocument();
  });

  it('invalidates the session and clears cart data after a 401 response', async () => {
    vi.mocked(cartApi.getCart).mockRejectedValue(Object.assign(new Error('Entre na sua conta.'), { kind: 'unauthenticated' }));
    setAuthenticatedSession();
    useCartStore.setState({ items: [availableItem], status: 'idle' });
    renderCart();

    expect(await screen.findByText('Entre na sua conta para ver seu carrinho.')).toBeInTheDocument();
    expect(useAuthStore.getState().session).toBeNull();
    expect(useCartStore.getState().items).toEqual([]);
  });

  it('offers retry after a cart service error and then displays the confirmed response', async () => {
    const user = userEvent.setup();
    vi.mocked(cartApi.getCart)
      .mockRejectedValueOnce(new Error('O carrinho está temporariamente indisponível. Tente novamente.'))
      .mockResolvedValueOnce(response(availableItem));
    setAuthenticatedSession();
    renderCart();

    expect(await screen.findByRole('heading', { name: 'Não foi possível carregar o carrinho.' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    expect(await screen.findByText('Keyboard')).toBeInTheDocument();
    expect(cartApi.getCart).toHaveBeenCalledTimes(2);
  });

  it('preserves confirmed rows and announces a failed quantity update', async () => {
    const user = userEvent.setup();
    vi.mocked(cartApi.setQuantity).mockRejectedValue(new Error('A atualização falhou.'));
    setAuthenticatedSession();
    renderCart();
    await screen.findByText('Keyboard');

    await user.click(screen.getByRole('button', { name: 'Aumentar quantidade de Keyboard' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('A atualização falhou.');
    expect(screen.getByRole('spinbutton', { name: 'Quantidade de Keyboard' })).toHaveValue(2);
    expect(screen.getByText('Keyboard')).toBeInTheDocument();
  });
});
