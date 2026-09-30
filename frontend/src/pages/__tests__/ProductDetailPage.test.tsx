import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cartApi, catalogApi } from '../../services/apiClient';
import { useAuthStore } from '../../store/authStore';
import { useCartStore } from '../../store/cartStore';
import { useCatalogStore } from '../../store/catalogStore';
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
    vi.mocked(cartApi.addItem).mockResolvedValue({ items: [{
      productId: product.id, quantity: 2, available: true,
      product: { name: product.name, price: product.price, brand: product.brand, imageUrl: product.imageUrl },
    }] });
  });

  it('adds the selected quantity once and does not block an active product with zero stock', async () => {
    const user = userEvent.setup();
    let resolveAdd: ((value: { items: [] }) => void) | undefined;
    vi.mocked(cartApi.addItem).mockReturnValue(new Promise((resolve) => { resolveAdd = resolve; }));
    renderProductDetail();

    expect(await screen.findByRole('heading', { name: 'Fone Studio' })).toBeInTheDocument();
    const quantity = screen.getByRole('spinbutton', { name: 'Quantidade' });
    await user.clear(quantity);
    await user.type(quantity, '2');
    const addButton = screen.getByRole('button', { name: 'Adicionar ao carrinho' });
    await user.click(addButton);

    await waitFor(() => expect(cartApi.addItem).toHaveBeenCalledWith({ productId: 'p-1', quantity: 2 }));
    expect(addButton).toBeDisabled();
    await user.click(addButton);
    expect(cartApi.addItem).toHaveBeenCalledTimes(1);

    resolveAdd?.({ items: [] });
    expect(await screen.findByRole('status')).toHaveTextContent('Produto adicionado ao carrinho.');
  });
});