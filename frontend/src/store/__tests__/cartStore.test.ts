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
};

const unavailableItem: CartItem = {
  productId: 'product-2',
  quantity: 1,
  available: false,
  product: null,
};

const response = (...items: CartItem[]): CartResponse => ({ items });

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
    vi.clearAllMocks();
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

  it('does not allow an older GET response to overwrite a confirmed mutation', async () => {
    const pending = deferred<CartResponse>();
    vi.mocked(cartApi.getCart).mockReturnValue(pending.promise);
    setAuthenticatedSession();

    const initialLoad = useCartStore.getState().loadCart();
    await useCartStore.getState().addItem({ productId: 'product-1', quantity: 2 });
    pending.resolve(response());
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
