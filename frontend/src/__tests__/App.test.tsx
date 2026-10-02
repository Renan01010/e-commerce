import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { cartApi, catalogApi } from '../services/apiClient';
import type { CartItem } from '../types/cart';
import { useAuthStore } from '../store/authStore';
import { useCartStore } from '../store/cartStore';
import App from '../App';

vi.mock('../services/apiClient', () => ({
  catalogApi: { getProducts: vi.fn(), getCategories: vi.fn(), getProduct: vi.fn() },
  cartApi: { getCart: vi.fn(), addItem: vi.fn(), setQuantity: vi.fn(), removeItem: vi.fn(), clearCart: vi.fn() },
  getApiErrorMessage: () => 'Erro do catálogo',
}));

const cartItems: CartItem[] = [
  { productId: 'product-1', quantity: 2, available: true, product: { name: 'Keyboard', price: 49.9, brand: 'Acme', imageUrl: null }, unitPriceSnapshot: 49.9, priceAvailable: true, subtotal: 99.8 },
  { productId: 'product-2', quantity: 1, available: false, product: null, unitPriceSnapshot: null, priceAvailable: false, subtotal: null },
];

describe('App cart navigation and badge', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    useCartStore.setState({ items: [], status: 'idle', error: null, successMessage: null, pendingOperations: {} });
    vi.clearAllMocks();
    vi.mocked(catalogApi.getProducts).mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 20, hasMore: false });
    vi.mocked(catalogApi.getCategories).mockResolvedValue([]);
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: cartItems, maxItemQuantity: 99, total: null, totalAvailable: false });
  });

  it('shows the sum of all quantities and navigates to the cart without another GET', async () => {
    const user = userEvent.setup();
    useAuthStore.getState().setSession({ accessToken: 'valid-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 });
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>);

    const cartLink = await screen.findByRole('link', { name: 'Carrinho, 3 unidades' });
    expect(cartLink).toHaveAttribute('href', '/cart');
    await user.click(cartLink);
    expect(await screen.findByRole('heading', { name: 'Seu carrinho' })).toBeInTheDocument();
    expect(cartApi.getCart).toHaveBeenCalledTimes(1);
  });
});
