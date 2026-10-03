import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cartApi, catalogApi } from '../../services/apiClient';
import { useAuthStore } from '../../store/authStore';
import { useCartStore } from '../../store/cartStore';
import { useCatalogStore } from '../../store/catalogStore';
import type { CartResponse } from '../../types/cart';
import { ProductDetailPage } from '../ProductDetailPage';

vi.mock('../../services/apiClient', () => ({
  catalogApi: { getProducts: vi.fn(), getCategories: vi.fn(), getProduct: vi.fn() },
  cartApi: { getCart: vi.fn(), addItem: vi.fn(), setQuantity: vi.fn(), removeItem: vi.fn(), clearCart: vi.fn() },
  getApiErrorMessage: () => 'Produto indisponível',
}));

const product = {
  id: 'p-1', name: 'Fone Studio', description: 'Som sem ruído', price: 399.9, cost: null,
  brand: 'Acme', sku: 'AC-1', categoryId: 'c-1', quantity: 0, imageUrl: null,
  isActive: true, createdAt: '', updatedAt: '',
};

function renderProductDetail() {
  return render(<MemoryRouter initialEntries={['/products/p-1']}><Routes>
    <Route path="/products/:id" element={<ProductDetailPage />} />
  </Routes></MemoryRouter>);
}

describe('ProductDetailPage', () => {
  beforeEach(() => {
    useCatalogStore.setState({ selectedProduct: null, categories: [], isLoading: false, error: null });
    useCartStore.setState({ items: [], status: 'idle', error: null, successMessage: null, pendingOperations: {} });
    useAuthStore.getState().setSession({ accessToken: 'valid-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 });
    vi.mocked(catalogApi.getCategories).mockResolvedValue([]);
    vi.mocked(catalogApi.getProduct).mockResolvedValue(product);
    vi.mocked(cartApi.getCart).mockReset();
    vi.mocked(cartApi.addItem).mockReset();
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], maxItemQuantity: 99, total: 0, totalAvailable: true });
    vi.mocked(cartApi.addItem).mockResolvedValue({ items: [{
      productId: product.id, quantity: 2, available: true,
      product: { name: product.name, price: product.price, brand: product.brand, imageUrl: product.imageUrl },
      unitPriceSnapshot: product.price, priceAvailable: true, subtotal: product.price * 2,
    }], maxItemQuantity: 99, total: product.price * 2, totalAvailable: true });
  });

  it('adds the selected quantity once and does not block an active product with zero stock', async () => {
    const user = userEvent.setup();
    let resolveAdd: ((value: CartResponse) => void) | undefined;
    vi.mocked(cartApi.addItem).mockReturnValue(new Promise((resolve) => { resolveAdd = resolve; }));
    renderProductDetail();

    expect(await screen.findByRole('heading', { name: 'Fone Studio' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '← Voltar ao catálogo' })).toHaveAttribute('href', '/catalog');
    const addButton = await screen.findByRole('button', { name: 'Adicionar ao carrinho' });
    await waitFor(() => expect(addButton).toBeEnabled());
    const quantity = screen.getByRole('spinbutton', { name: 'Quantidade' });
    await user.clear(quantity);
    await user.type(quantity, '2');
    await user.click(addButton);

    await waitFor(() => expect(cartApi.addItem).toHaveBeenCalledWith({ productId: 'p-1', quantity: 2 }));
    expect(addButton).toBeDisabled();
    await user.click(addButton);
    expect(cartApi.addItem).toHaveBeenCalledTimes(1);

    resolveAdd?.({ items: [], maxItemQuantity: 99, total: 0, totalAvailable: true });
    expect(await screen.findByRole('status')).toHaveTextContent('Produto adicionado ao carrinho.');
  });

  it('keeps add disabled until the official quantity limit has loaded', async () => {
    const user = userEvent.setup();
    let resolveCart: ((value: CartResponse) => void) | undefined;
    vi.mocked(cartApi.getCart).mockReturnValue(new Promise((resolve) => { resolveCart = resolve; }));
    renderProductDetail();

    expect(await screen.findByRole('heading', { name: 'Fone Studio' })).toBeInTheDocument();
    const addButton = screen.getByRole('button', { name: /Adicionar ao carrinho|Carregando limite/i });
    expect(addButton).toBeDisabled();
    await user.click(addButton);
    expect(cartApi.addItem).not.toHaveBeenCalled();

    resolveCart?.({ items: [], maxItemQuantity: 4, total: 0, totalAvailable: true });
    await waitFor(() => expect(screen.getByRole('button', { name: 'Adicionar ao carrinho' })).toBeEnabled());

    const quantity = screen.getByRole('spinbutton', { name: 'Quantidade' });
    await user.clear(quantity);
    await user.type(quantity, '5');
    expect(screen.getByRole('button', { name: 'Adicionar ao carrinho' })).toBeDisabled();
    expect(cartApi.addItem).not.toHaveBeenCalled();
  });

  it('keeps add disabled after a failed cart load and enables it only after retry succeeds', async () => {
    const user = userEvent.setup();
    vi.mocked(cartApi.getCart)
      .mockRejectedValueOnce(new Error('Carrinho indisponível'))
      .mockResolvedValueOnce({ items: [], maxItemQuantity: 4, total: 0, totalAvailable: true });
    renderProductDetail();

    expect(await screen.findByRole('heading', { name: 'Fone Studio' })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Adicionar ao carrinho' })).toBeDisabled();
    const retry = await screen.findByRole('button', { name: 'Tentar carregar carrinho' });
    await user.click(retry);

    await waitFor(() => expect(screen.getByRole('button', { name: 'Adicionar ao carrinho' })).toBeEnabled());
    expect(cartApi.getCart).toHaveBeenCalledTimes(2);
    expect(cartApi.addItem).not.toHaveBeenCalled();
  });
});