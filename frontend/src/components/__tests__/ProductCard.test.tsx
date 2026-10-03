import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ProductCard } from '../ProductCard';
import type { Product } from '../../types/catalog';
import { cartApi } from '../../services/apiClient';
import { useAuthStore } from '../../store/authStore';
import { useCartStore } from '../../store/cartStore';

vi.mock('../../services/apiClient', () => ({
  catalogApi: { getProducts: vi.fn(), getCategories: vi.fn(), getProduct: vi.fn() },
  cartApi: { getCart: vi.fn(), addItem: vi.fn(), setQuantity: vi.fn(), removeItem: vi.fn(), clearCart: vi.fn() },
  getApiErrorMessage: () => 'Erro do catálogo',
}));

const product: Product = {
  id: 'p-1', name: 'Fone Studio', description: 'Som sem ruído', price: 399.9, cost: null,
  brand: 'Acme', sku: 'AC-1', categoryId: 'c-1', quantity: 3, imageUrl: null,
  isActive: true, createdAt: '', updatedAt: '',
};

describe('ProductCard', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    useCartStore.setState({ items: [], status: 'idle', error: null, successMessage: null, pendingOperations: {} });
    vi.clearAllMocks();
  });

  afterEach(() => useAuthStore.getState().clearSession());

  it('shows product details and links to its page', () => {
    render(<MemoryRouter><ProductCard product={product} categoryName="Áudio" /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Fone Studio' })).toBeInTheDocument();
    expect(screen.getByText((text) => text.replace(/\s/g, ' ') === 'R$ 399,90')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver Fone Studio' })).toHaveAttribute('href', '/products/p-1');
    expect(screen.getByText('Disponível')).toBeInTheDocument();
    expect(screen.getByText('Áudio')).toBeInTheDocument();
    expect(screen.getByText('Acme')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Imagem indisponível para Fone Studio' })).toBeInTheDocument();
    expect(screen.queryByText('p-1')).not.toBeInTheDocument();
  });

  it('does not offer a cart mutation to a visitor', () => {
    render(<MemoryRouter><ProductCard product={product} /></MemoryRouter>);

    expect(screen.getByRole('link', { name: /Entre na sua conta para adicionar Fone Studio/ })).toHaveAttribute('href', '/login');
    expect(screen.queryByRole('button', { name: 'Adicionar Fone Studio ao carrinho' })).not.toBeInTheDocument();
  });

  it('replaces a broken product image with an accessible fallback', () => {
    render(<MemoryRouter><ProductCard product={{ ...product, imageUrl: 'https://example.test/product.webp' }} /></MemoryRouter>);

    fireEvent.error(screen.getByRole('img', { name: 'Fone Studio' }));
    expect(screen.getByRole('img', { name: 'Imagem indisponível para Fone Studio' })).toBeInTheDocument();
  });

  it('uses the cart service, prevents duplicate submissions while loading, and reports confirmation', async () => {
    const user = userEvent.setup();
    useAuthStore.getState().setSession({ accessToken: 'token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 });
    useCartStore.setState({ status: 'loaded' });
    let resolveAdd: ((response: { items: []; maxItemQuantity: number; total: number; totalAvailable: boolean }) => void) | undefined;
    vi.mocked(cartApi.addItem).mockReturnValue(new Promise((resolve) => { resolveAdd = resolve; }));
    render(<MemoryRouter><ProductCard product={product} /></MemoryRouter>);

    const add = screen.getByRole('button', { name: 'Adicionar Fone Studio ao carrinho' });
    await user.click(add);
    expect(cartApi.addItem).toHaveBeenCalledTimes(1);
    expect(cartApi.addItem).toHaveBeenCalledWith({ productId: 'p-1', quantity: 1 });
    const pendingButton = screen.getByRole('button', { name: 'Adicionar Fone Studio ao carrinho' });
    expect(pendingButton).toBeDisabled();
    expect(pendingButton).toHaveTextContent('Adicionando…');
    await user.click(pendingButton);
    expect(cartApi.addItem).toHaveBeenCalledTimes(1);

    resolveAdd?.({ items: [], maxItemQuantity: 99, total: 0, totalAvailable: true });
    expect(await screen.findByRole('status')).toHaveTextContent('Produto adicionado ao carrinho.');
  });

  it('shows a cart failure without claiming the product was added', async () => {
    const user = userEvent.setup();
    useAuthStore.getState().setSession({ accessToken: 'token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 });
    useCartStore.setState({ status: 'loaded' });
    vi.mocked(cartApi.addItem).mockRejectedValue(new Error('Não foi possível adicionar.'));
    render(<MemoryRouter><ProductCard product={product} /></MemoryRouter>);

    await user.click(screen.getByRole('button', { name: 'Adicionar Fone Studio ao carrinho' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível adicionar.');
  });

  it('keeps an unavailable product disabled and labels its stock state', () => {
    useAuthStore.getState().setSession({ accessToken: 'token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 });
    useCartStore.setState({ status: 'loaded' });
    render(<MemoryRouter><ProductCard product={{ ...product, quantity: 0 }} /></MemoryRouter>);

    expect(screen.getByText('Esgotado')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Adicionar Fone Studio ao carrinho' })).toBeDisabled();
    expect(cartApi.addItem).not.toHaveBeenCalled();
  });
});