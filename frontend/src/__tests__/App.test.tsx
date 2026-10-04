import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { accountService } from '../services/accountService';
import { cartApi, catalogApi } from '../services/apiClient';
import type { CartItem } from '../types/cart';
import type { UserProfile } from '../types/auth';
import { useAuthStore } from '../store/authStore';
import { useCartStore } from '../store/cartStore';
import { useCatalogStore } from '../store/catalogStore';
import App from '../App';

vi.mock('../services/apiClient', () => ({
  catalogApi: { getProducts: vi.fn(), getCategories: vi.fn(), getProduct: vi.fn() },
  cartApi: { getCart: vi.fn(), addItem: vi.fn(), setQuantity: vi.fn(), removeItem: vi.fn(), clearCart: vi.fn() },
  getApiErrorMessage: () => 'Erro do catálogo',
}));

vi.mock('../services/accountService', () => ({
  accountService: { getProfile: vi.fn(), updateName: vi.fn(), changePassword: vi.fn() },
}));

const userProfile: UserProfile = {
  id: 'user-1', name: 'Ana Silva', email: 'ana@example.com', emailVerified: true,
};

const cartItems: CartItem[] = [
  { productId: 'product-1', quantity: 2, available: true, product: { name: 'Keyboard', price: 49.9, brand: 'Acme', imageUrl: null }, unitPriceSnapshot: 49.9, priceAvailable: true, subtotal: 99.8 },
  { productId: 'product-2', quantity: 1, available: false, product: null, unitPriceSnapshot: null, priceAvailable: false, subtotal: null },
];

describe('App cart navigation and badge', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    useCartStore.setState({ items: [], status: 'idle', error: null, successMessage: null, pendingOperations: {} });
    useCatalogStore.setState({
      categories: [], categoriesStatus: 'idle', categoriesError: null,
      recentProducts: [], recentProductsLoading: false, recentProductsStatus: 'idle', recentProductsError: null,
    });
    vi.clearAllMocks();
    vi.mocked(accountService.getProfile).mockResolvedValue(userProfile);
    vi.mocked(catalogApi.getProducts).mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 20, hasMore: false });
    vi.mocked(catalogApi.getCategories).mockResolvedValue([]);
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: cartItems, maxItemQuantity: 99, total: null, totalAvailable: false });
  });

  it('uses the Home at / and preserves the full catalog at /catalog', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: /Tecnologia sem limites/ })).toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Produtos' }));
    expect(await screen.findByRole('heading', { name: 'Encontre algo que acompanhe seu ritmo.' })).toBeInTheDocument();
  });

  it('uses the Home search there and keeps the header search on the catalog route', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>);

    expect(screen.getByRole('searchbox', { name: 'Buscar produtos no catálogo' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Buscar no catálogo' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('link', { name: 'Produtos' }));
    expect(await screen.findByRole('button', { name: 'Buscar no catálogo' })).toBeInTheDocument();
    expect(within(screen.getByRole('search')).getByRole('searchbox', { name: 'Buscar produtos' })).toBeInTheDocument();
  });

  it('exposes an accessible responsive menu with working catalog and category navigation', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>);

    const menu = screen.getByRole('button', { name: 'Abrir menu' });
    expect(menu).toHaveAttribute('aria-expanded', 'false');
    await user.click(menu);
    expect(screen.getByRole('button', { name: 'Fechar menu' })).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('navigation', { name: 'Navegação principal' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Entrar na minha conta' })).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: 'Carrinho' })).toHaveAttribute('href', '/cart');
  });

  it('sends the not-found fallback to the full catalog', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter initialEntries={['/missing-page']}><App /></MemoryRouter>);

    expect(screen.getByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Ir ao catálogo' }));
    expect(await screen.findByRole('heading', { name: 'Encontre algo que acompanhe seu ritmo.' })).toBeInTheDocument();
  });

  it('keeps authentication routes standalone while the store header remains shared elsewhere', () => {
    const { unmount } = render(<MemoryRouter initialEntries={['/login']}><App /></MemoryRouter>);
    expect(screen.getByRole('main')).toHaveClass('login-screen');
    expect(screen.queryByRole('banner')).not.toBeInTheDocument();
    unmount();

    render(<MemoryRouter initialEntries={['/register']}><App /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Crie sua conta' })).toBeInTheDocument();
    expect(screen.queryByRole('banner')).not.toBeInTheDocument();
  });

  it('guards account routes and preserves their safe destination through login', async () => {
    render(<MemoryRouter initialEntries={['/account/security?tab=password']}><App /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: 'Acesse sua conta' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Segurança' })).not.toBeInTheDocument();
  });

  it('treats an expired in-memory session as unauthenticated for account routes', async () => {
    useAuthStore.getState().setSession({
      accessToken: 'expired-token', tokenType: 'Bearer', expiresAt: Date.now() - 1,
    });
    render(<MemoryRouter initialEntries={['/account']}><App /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: 'Acesse sua conta' })).toBeInTheDocument();
    expect(useAuthStore.getState().session).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Minha conta' })).not.toBeInTheDocument();
  });

  it('loads the authenticated profile for the header without blocking the store', async () => {
    let resolveProfile!: (profile: UserProfile) => void;
    vi.mocked(accountService.getProfile).mockImplementation(() => new Promise((resolve) => {
      resolveProfile = resolve;
    }));
    useAuthStore.getState().setSession({
      accessToken: 'valid-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000,
    });
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: /Tecnologia sem limites/ })).toBeInTheDocument();
    expect(screen.getByText('Carregando...')).toHaveAttribute('aria-busy', 'true');
    expect(accountService.getProfile).toHaveBeenCalledTimes(1);

    resolveProfile(userProfile);
    expect(await screen.findByText('Ana Silva')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Minha conta' })).toBeInTheDocument();
  });

  it('logs out locally without calling the destructive cart endpoint', async () => {
    const user = userEvent.setup();
    useAuthStore.getState().setSession({
      accessToken: 'valid-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000,
    });
    useCartStore.setState({ items: cartItems, status: 'loaded' });
    useCatalogStore.setState({
      products: [{ id: 'private-product' } as never],
      recentProducts: [{ id: 'private-recent-product' } as never],
      selectedProduct: { id: 'private-selected-product' } as never,
    });
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>);

    await user.click(await screen.findByRole('button', { name: 'Sair' }));

    expect(useAuthStore.getState().session).toBeNull();
    expect(useCartStore.getState().items).toEqual([]);
    expect(useCartStore.getState().status).toBe('idle');
    expect(useCartStore.getState().total).toBeNull();
    expect(useCatalogStore.getState().products).toEqual([]);
    expect(useCatalogStore.getState().recentProducts).toEqual([]);
    expect(useCatalogStore.getState().selectedProduct).toBeNull();
    expect(await screen.findByRole('heading', { name: /Tecnologia sem limites/ })).toBeInTheDocument();
    expect(cartApi.clearCart).not.toHaveBeenCalled();
    expect(screen.getByRole('link', { name: 'Criar conta' })).toHaveAttribute('href', '/register');
  });

  it('updates the header badge after a confirmed add initiated from a Home product card', async () => {
    const user = userEvent.setup();
    useAuthStore.getState().setSession({ accessToken: 'valid-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 });
    const addedItem: CartItem = {
      productId: 'new-product', quantity: 1, available: true,
      product: { name: 'Newest', price: 15, brand: 'Acme', imageUrl: null },
      unitPriceSnapshot: 15, priceAvailable: true, subtotal: 15,
    };
    vi.mocked(cartApi.getCart).mockResolvedValue({ items: [], maxItemQuantity: 99, total: 0, totalAvailable: true });
    vi.mocked(cartApi.addItem).mockResolvedValue({
      items: [addedItem], maxItemQuantity: 99, total: 15, totalAvailable: true,
    });
    vi.mocked(catalogApi.getProducts).mockResolvedValue({
      content: [{
        id: 'new-product', name: 'Newest', description: null, price: 15, cost: null, brand: 'Acme',
        sku: 'N-1', categoryId: 'category-1', quantity: 2, imageUrl: null, isActive: true, createdAt: '', updatedAt: '',
      }],
      totalElements: 1, totalPages: 1, currentPage: 0, pageSize: 8, hasMore: false,
    });
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>);

    expect(await screen.findByRole('link', { name: 'Carrinho, 0 unidades' })).toBeInTheDocument();
    await user.click(await screen.findByRole('button', { name: 'Adicionar Newest ao carrinho' }));
    expect(await screen.findByRole('link', { name: 'Carrinho, 1 unidade' })).toBeInTheDocument();
    expect(cartApi.addItem).toHaveBeenCalledTimes(1);
    expect(useCartStore.getState().total).toBe(15);
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

  it('updates the header badge after every confirmed mutation and keeps server totals authoritative', async () => {
    useAuthStore.getState().setSession({ accessToken: 'valid-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 });
    const cartResponse = (items: CartItem[], total: number) => ({
      items, maxItemQuantity: 99, total, totalAvailable: true,
    });
    vi.mocked(cartApi.getCart)
      .mockResolvedValueOnce(cartResponse(cartItems, 1000.01))
      .mockResolvedValueOnce(cartResponse([{ ...cartItems[0], quantity: 1 }], 17.23))
      .mockResolvedValueOnce(cartResponse([], 0));
    vi.mocked(cartApi.addItem).mockResolvedValueOnce(cartResponse([
      { ...cartItems[0], quantity: 3 }, cartItems[1],
    ], 987.65));
    vi.mocked(cartApi.setQuantity)
      .mockResolvedValueOnce(cartResponse([{ ...cartItems[0], quantity: 4 }, cartItems[1]], 456.78))
      .mockResolvedValueOnce(cartResponse([{ ...cartItems[0], quantity: 1 }, cartItems[1]], 321.09));
    vi.mocked(cartApi.removeItem).mockResolvedValue(undefined);
    vi.mocked(cartApi.clearCart).mockResolvedValue(undefined);
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>);

    expect(await screen.findByRole('link', { name: 'Carrinho, 3 unidades' })).toBeInTheDocument();

    await useCartStore.getState().addItem({ productId: 'product-1', quantity: 1 });
    expect(await screen.findByRole('link', { name: 'Carrinho, 4 unidades' })).toBeInTheDocument();
    expect(useCartStore.getState().total).toBe(987.65);

    await useCartStore.getState().setQuantity('product-1', 4);
    expect(await screen.findByRole('link', { name: 'Carrinho, 5 unidades' })).toBeInTheDocument();
    expect(useCartStore.getState().total).toBe(456.78);

    await useCartStore.getState().setQuantity('product-1', 1);
    expect(await screen.findByRole('link', { name: 'Carrinho, 2 unidades' })).toBeInTheDocument();
    expect(useCartStore.getState().total).toBe(321.09);

    await useCartStore.getState().removeItem('product-2');
    expect(await screen.findByRole('link', { name: 'Carrinho, 1 unidade' })).toBeInTheDocument();
    expect(useCartStore.getState().total).toBe(17.23);

    await useCartStore.getState().clearCart();
    expect(await screen.findByRole('link', { name: 'Carrinho, 0 unidades' })).toBeInTheDocument();
    expect(useCartStore.getState().total).toBe(0);
    expect(cartApi.addItem).toHaveBeenCalledTimes(1);
    expect(cartApi.setQuantity).toHaveBeenCalledTimes(2);
    expect(cartApi.removeItem).toHaveBeenCalledTimes(1);
    expect(cartApi.clearCart).toHaveBeenCalledTimes(1);
    expect(cartApi.getCart).toHaveBeenCalledTimes(3);
  });

  it('clears cart state in the header when its session is rejected with 401', async () => {
    vi.mocked(cartApi.getCart).mockRejectedValue(Object.assign(new Error('Authentication required'), {
      kind: 'unauthenticated',
    }));
    useAuthStore.getState().setSession({ accessToken: 'expired-token', tokenType: 'Bearer', expiresAt: Date.now() + 60_000 });
    useCartStore.setState({ items: cartItems, status: 'idle' });
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>);

    expect(await screen.findByRole('link', { name: 'Carrinho' })).toBeInTheDocument();
    expect(useAuthStore.getState().session).toBeNull();
    expect(useCartStore.getState().items).toEqual([]);
  });
});
