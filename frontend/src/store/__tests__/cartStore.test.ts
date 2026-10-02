import { beforeEach, describe, expect, it, vi } from 'vitest';
import { cartApi } from '../../services/apiClient';
import type { CartItem, CartResponse } from '../../types/cart';
import { useAuthStore } from '../authStore';
import { useCartStore } from '../cartStore';

vi.mock('../../services/apiClient', () => ({
  cartApi: {
    getCart: vi.fn(),
    addItem: vi.fn(),
    setQuantity: vi.fn(),
    removeItem: vi.fn(),
    clearCart: vi.fn(),
  },
}));

const availableItem: CartItem = {
  productId: 'product-1',
  quantity: 2,
  available: true,
  product: { name: 'Keyboard', price: 49.9, brand: 'Acme', imageUrl: null },
  unitPriceSnapshot: 49.9,
  priceAvailable: true,
  subtotal: 99.8,
};

const unavailableItem: CartItem = {
  productId: 'product-2',
  quantity: 1,
  available: false,
  product: null,
  unitPriceSnapshot: null,
  priceAvailable: false,
  subtotal: null,
};

const response = (...items: CartItem[]): CartResponse => ({
  items,
  maxItemQuantity: 99,
  total: items.some((item) => !item.priceAvailable) ? null : items.reduce((sum, item) => sum + (item.subtotal ?? 0), 0),
  totalAvailable: items.every((item) => item.priceAvailable),
});

function setAuthenticatedSession() {
  useAuthStore.getState().setSession({
    accessToken: 'session-token',
    tokenType: 'Bearer',
    expiresAt: Date.now() + 60_000,
  });
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

describe('cartStore', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    useCartStore.setState({
      items: [], status: 'idle', error: null, successMessage: null, pendingOperations: {},
    });
    vi.resetAllMocks();
    vi.mocked(cartApi.getCart).mockResolvedValue(response());
    vi.mocked(cartApi.addItem).mockResolvedValue(response(availableItem));
    vi.mocked(cartApi.setQuantity).mockResolvedValue(response({ ...availableItem, quantity: 3 }));
    vi.mocked(cartApi.removeItem).mockResolvedValue(undefined);
    vi.mocked(cartApi.clearCart).mockResolvedValue(undefined);
  });

  it('deduplicates simultaneous initial loads for the same session', async () => {
    const pending = deferred<CartResponse>();
    vi.mocked(cartApi.getCart).mockReturnValue(pending.promise);
    setAuthenticatedSession();

    const firstLoad = useCartStore.getState().loadCart();
    const secondLoad = useCartStore.getState().loadCart();
    expect(cartApi.getCart).toHaveBeenCalledTimes(1);

    pending.resolve(response(availableItem));
    await Promise.all([firstLoad, secondLoad]);
    expect(useCartStore.getState().items).toEqual([availableItem]);
    expect(useCartStore.getState().status).toBe('loaded');
  });

  it('uses the total returned by the service instead of summing item subtotals', async () => {
    const serverResponse = { ...response(availableItem), total: 123.45 };
    vi.mocked(cartApi.getCart).mockResolvedValue(serverResponse);
    setAuthenticatedSession();

    await useCartStore.getState().loadCart();

    expect(useCartStore.getState().items).toEqual([availableItem]);
    expect(useCartStore.getState().total).toBe(123.45);
    expect(useCartStore.getState().totalAvailable).toBe(true);
  });

  it('fetches the authoritative financial summary after clearing', async () => {
    setAuthenticatedSession();
    useCartStore.setState({ items: [availableItem], total: 99.8, totalAvailable: true, status: 'loaded' });
    vi.mocked(cartApi.getCart).mockResolvedValue(response());

    await useCartStore.getState().clearCart();

    expect(cartApi.clearCart).toHaveBeenCalledTimes(1);
    expect(cartApi.getCart).toHaveBeenCalledTimes(1);
    expect(useCartStore.getState().items).toEqual([]);
    expect(useCartStore.getState().total).toBe(0);
    expect(useCartStore.getState().totalAvailable).toBe(true);
  });

  it('keeps a confirmed removal after summary GET fails and retries only the GET', async () => {
    setAuthenticatedSession();
    useCartStore.setState({ items: [availableItem, unavailableItem], status: 'loaded', total: null, totalAvailable: false });
    vi.mocked(cartApi.getCart)
      .mockRejectedValueOnce(new Error('Resumo temporariamente indisponível'))
      .mockResolvedValueOnce(response(availableItem));

    const removed = await useCartStore.getState().removeItem(unavailableItem.productId);

    expect(removed).toBe(true);
    expect(cartApi.removeItem).toHaveBeenCalledTimes(1);
    expect(useCartStore.getState().items).toEqual([availableItem]);
    expect(useCartStore.getState().total).toBeNull();
    expect(useCartStore.getState().totalAvailable).toBe(false);
    expect(useCartStore.getState().summaryRefreshError).toMatch(/Resumo temporariamente indisponível/);

    await useCartStore.getState().retrySummary();

    expect(cartApi.removeItem).toHaveBeenCalledTimes(1);
    expect(cartApi.getCart).toHaveBeenCalledTimes(2);
    expect(useCartStore.getState().items).toEqual([availableItem]);
    expect(useCartStore.getState().total).toBe(99.8);
    expect(useCartStore.getState().totalAvailable).toBe(true);
    expect(useCartStore.getState().summaryRefreshError).toBeNull();
  });

  it('replaces cart state from the confirmed mutation response', async () => {
    setAuthenticatedSession();
    useCartStore.setState({ items: [availableItem], status: 'loaded' });
    vi.mocked(cartApi.addItem).mockResolvedValue(response(availableItem, unavailableItem));

    await useCartStore.getState().addItem({ productId: 'product-2', quantity: 1 });

    expect(useCartStore.getState().items).toEqual([availableItem, unavailableItem]);
    expect(useCartStore.getState().successMessage).toBeTruthy();
  });

  it('preserves the last confirmed lines when a mutation fails', async () => {
    setAuthenticatedSession();
    useCartStore.setState({ items: [availableItem], status: 'loaded' });
    vi.mocked(cartApi.setQuantity).mockRejectedValue(new Error('Request failed'));

    await useCartStore.getState().setQuantity('product-1', 4);

    expect(useCartStore.getState().items).toEqual([availableItem]);
    expect(useCartStore.getState().error).toBe('Request failed');
    expect(useCartStore.getState().pendingOperations).toEqual({});
  });

  it('refreshes the server total after removing an unknown-price line', async () => {
    setAuthenticatedSession();
    useCartStore.setState({ items: [availableItem, unavailableItem], status: 'loaded' });
    vi.mocked(cartApi.getCart).mockResolvedValueOnce(response(availableItem));

    await useCartStore.getState().removeItem(unavailableItem.productId);

    expect(cartApi.removeItem).toHaveBeenCalledWith(unavailableItem.productId);
    expect(cartApi.getCart).toHaveBeenCalledTimes(1);
    expect(useCartStore.getState().items).toEqual([availableItem]);
    expect(useCartStore.getState().total).toBe(99.8);
    expect(useCartStore.getState().totalAvailable).toBe(true);
  });

  it('rejects a quantity above the effective cart maximum before calling the API', async () => {
    setAuthenticatedSession();
    useCartStore.setState({ maxItemQuantity: 4, status: 'loaded' });

    const succeeded = await useCartStore.getState().addItem({ productId: 'product-1', quantity: 5 });

    expect(succeeded).toBe(false);
    expect(cartApi.addItem).not.toHaveBeenCalled();
    expect(useCartStore.getState().error).toMatch(/máxim|limite/i);
  });

  it('does not use the initial maximum before a cart response is loaded', async () => {
    setAuthenticatedSession();
    useCartStore.setState({ status: 'idle', maxItemQuantity: 99 });

    const succeeded = await useCartStore.getState().addItem({ productId: 'product-1', quantity: 1 });

    expect(succeeded).toBe(false);
    expect(cartApi.addItem).not.toHaveBeenCalled();
    expect(useCartStore.getState().error).toMatch(/carregamento do carrinho/i);
  });

  it('does not allow a mutation while the initial cart response is pending', async () => {
    const pending = deferred<CartResponse>();
    vi.mocked(cartApi.getCart).mockReturnValue(pending.promise);
    setAuthenticatedSession();

    const initialLoad = useCartStore.getState().loadCart();
    const succeeded = await useCartStore.getState().addItem({ productId: 'product-1', quantity: 2 });

    expect(succeeded).toBe(false);
    expect(cartApi.addItem).not.toHaveBeenCalled();
    pending.resolve(response(availableItem));
    await initialLoad;

    expect(useCartStore.getState().items).toEqual([availableItem]);
  });

  it('clears cart data when the authenticated session is replaced', async () => {
    setAuthenticatedSession();
    useCartStore.setState({ items: [availableItem], status: 'loaded' });

    setAuthenticatedSession();

    expect(useCartStore.getState().items).toEqual([]);
    expect(useCartStore.getState().status).toBe('idle');
  });
});
