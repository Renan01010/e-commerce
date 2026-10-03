import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from '../apiClient';
import { useAuthStore } from '../../store/authStore';

describe('apiClient authentication interceptor', () => {
  let originalAdapter: AxiosAdapter | undefined;
  let capturedConfig: InternalAxiosRequestConfig | undefined;

  afterEach(() => {
    apiClient.defaults.adapter = originalAdapter;
    useAuthStore.getState().clearSession();
    capturedConfig = undefined;
  });

  async function captureRequest() {
    originalAdapter = apiClient.defaults.adapter as AxiosAdapter | undefined;
    const adapter: AxiosAdapter = async (config) => {
      capturedConfig = config;
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config };
    };
    apiClient.defaults.adapter = adapter;
    await apiClient.get('/protected');
    return capturedConfig;
  }

  it('adds a bearer header for a valid in-memory session', async () => {
    useAuthStore.getState().setSession({
      accessToken: 'valid-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 60_000,
    });

    const config = await captureRequest();

    expect(config?.headers.get('Authorization')).toBe('Bearer valid-token');
  });

  it('does not add a bearer header without a session', async () => {
    const config = await captureRequest();

    expect(config?.headers.get('Authorization')).toBeUndefined();
  });

  it('clears expired session and does not send its token', async () => {
    useAuthStore.getState().setSession({
      accessToken: 'expired-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() - 1,
    });

    const config = await captureRequest();

    expect(config?.headers.get('Authorization')).toBeUndefined();
    expect(useAuthStore.getState().session).toBeNull();
  });
});

describe('catalogApi', () => {
  afterEach(() => vi.restoreAllMocks());

  it('sends the supported catalog parameters and omits empty filters', async () => {
    const request = vi.spyOn(apiClient, 'get').mockResolvedValue({
      data: { content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 20, hasMore: false },
    } as never);

    const { catalogApi } = await import('../apiClient');
    await catalogApi.getProducts({
      query: '', filters: { categoryId: 'category-1', inStock: true },
      sortBy: 'price', sortOrder: 'asc', page: 0, pageSize: 20,
    });

    expect(request).toHaveBeenCalledWith('/products', {
      params: {
        query: undefined, categoryId: 'category-1', minPrice: undefined, maxPrice: undefined,
        inStock: true, brand: undefined, sortBy: 'price', sortOrder: 'asc', page: 0, pageSize: 20,
      },
    });
  });
});

describe('catalog API error messages', () => {
  it.each([
    [400, 'Não foi possível concluir sua busca. Confira os filtros e tente novamente.'],
    [401, 'Sua sessão precisa ser atualizada. Tente novamente.'],
    [403, 'Você não tem permissão para acessar este conteúdo.'],
    [404, 'O conteúdo solicitado não está disponível.'],
    [409, 'Não foi possível concluir a solicitação. Atualize a página e tente novamente.'],
    [429, 'Muitas solicitações em pouco tempo. Aguarde um instante e tente novamente.'],
    [500, 'O catálogo está temporariamente indisponível. Tente novamente.'],
    [503, 'O catálogo está temporariamente indisponível. Tente novamente.'],
  ])('maps HTTP %i to a safe public message', async (status, message) => {
    const { getApiErrorMessage } = await import('../apiClient');
    const error = Object.assign(new Error('Sensitive technical details'), {
      isAxiosError: true,
      response: { status, data: { message: 'Sensitive technical details' } },
    });

    expect(getApiErrorMessage(error)).toBe(message);
  });

  it('does not expose technical messages for network or unknown errors', async () => {
    const { getApiErrorMessage } = await import('../apiClient');
    const networkError = Object.assign(new Error('Sensitive technical details'), { isAxiosError: true });

    expect(getApiErrorMessage(networkError)).toBe('Não foi possível conectar ao catálogo. Tente novamente.');
    expect(getApiErrorMessage(new Error('Sensitive technical details'))).toBe('O catálogo não pôde ser carregado.');
  });
});

describe('cartApi', () => {
  afterEach(() => vi.restoreAllMocks());

  it('loads the authenticated cart from the gateway path', async () => {
    const response = { items: [] };
    const request = vi.spyOn(apiClient, 'get').mockResolvedValue({ data: response } as never);

    await expect((await import('../apiClient')).cartApi.getCart()).resolves.toEqual(response);
    expect(request).toHaveBeenCalledWith('/cart');
  });

  it.each([200, 201])('accepts POST success status %i and sends only product and quantity', async (status) => {
    const response = { items: [] };
    const request = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: response, status } as never);
    const body = { productId: 'product-1', quantity: 2 };

    await expect((await import('../apiClient')).cartApi.addItem(body)).resolves.toEqual(response);
    expect(request).toHaveBeenCalledWith('/cart/items', body);
  });

  it('replaces quantity through the product-specific gateway path', async () => {
    const response = { items: [] };
    const request = vi.spyOn(apiClient, 'put').mockResolvedValue({ data: response } as never);

    await expect((await import('../apiClient')).cartApi.setQuantity('product-1', 4)).resolves.toEqual(response);
    expect(request).toHaveBeenCalledWith('/cart/items/product-1', { quantity: 4 });
  });

  it('treats item and cart DELETE 204 responses as bodyless success', async () => {
    const request = vi.spyOn(apiClient, 'delete').mockResolvedValue({ data: undefined, status: 204 } as never);
    const { cartApi } = await import('../apiClient');

    await expect(cartApi.removeItem('product-1')).resolves.toBeUndefined();
    await expect(cartApi.clearCart()).resolves.toBeUndefined();
    expect(request).toHaveBeenNthCalledWith(1, '/cart/items/product-1');
    expect(request).toHaveBeenNthCalledWith(2, '/cart');
  });

  it.each([
    [401, 'unauthenticated', 'Sua sessão precisa ser atualizada. Tente novamente.'],
    [404, 'not-found', 'O conteúdo solicitado não está disponível.'],
    [409, 'conflict', 'Não foi possível concluir a solicitação. Atualize a página e tente novamente.'],
    [503, 'unavailable', 'O catálogo está temporariamente indisponível. Tente novamente.'],
  ] as const)('classifies cart HTTP error %i and returns a safe catalog fallback', async (status, kind, message) => {
    const requestError = Object.assign(new Error('backend detail'), {
      isAxiosError: true,
      response: { status, data: { message: 'backend detail' } },
    });
    vi.spyOn(apiClient, 'get').mockRejectedValue(requestError);
    const { CartApiError, cartApi, getApiErrorMessage } = await import('../apiClient');

    await expect(cartApi.getCart()).rejects.toMatchObject({
      name: CartApiError.name,
      kind,
      status,
    });
    expect(getApiErrorMessage(requestError)).toBe(message);
  });

  it.each([
    [400, 'validation'],
    [403, 'forbidden'],
    [422, 'validation'],
    [500, 'server'],
  ] as const)('classifies cart HTTP error %i with a friendly fallback', async (status, kind) => {
    const requestError = Object.assign(new Error('backend detail'), {
      isAxiosError: true,
      response: { status, data: { message: 'Technical backend message' } },
    });
    vi.spyOn(apiClient, 'post').mockRejectedValue(requestError);
    const { cartApi } = await import('../apiClient');

    await expect(cartApi.addItem({ productId: 'product-1', quantity: 1 })).rejects.toMatchObject({
      kind,
      status,
    });
    await expect(cartApi.addItem({ productId: 'product-1', quantity: 1 })).rejects.not.toThrow(/HTTP 400|HTTP 403|HTTP 422|HTTP 500/);
  });

  it('preserves actionable ErrorResponse details when they are present', async () => {
    const requestError = Object.assign(new Error('backend detail'), {
      isAxiosError: true,
      response: { status: 409, data: { message: 'Conflict', details: 'Estoque disponível: 3.' } },
    });
    vi.spyOn(apiClient, 'put').mockRejectedValue(requestError);
    const { CartApiError, cartApi } = await import('../apiClient');

    await expect(cartApi.setQuantity('product-1', 4)).rejects.toMatchObject({
      name: CartApiError.name,
      kind: 'conflict',
      status: 409,
      message: 'Estoque disponível: 3.',
    });
  });

  it('preserves business conflict details from the cart service', async () => {
    const requestError = Object.assign(new Error('backend detail'), {
      isAxiosError: true,
      response: { status: 409, data: { message: 'Estoque disponível: 3.' } },
    });
    vi.spyOn(apiClient, 'post').mockRejectedValue(requestError);
    const { CartApiError, cartApi } = await import('../apiClient');

    await expect(cartApi.addItem({ productId: 'product-1', quantity: 4 })).rejects.toMatchObject({
      name: CartApiError.name,
      kind: 'conflict',
      status: 409,
      message: 'Estoque disponível: 3.',
    });
  });

  it('classifies a network failure as retryable cart unavailability', async () => {
    vi.spyOn(apiClient, 'get').mockRejectedValue(Object.assign(new Error('offline'), { isAxiosError: true }));
    const { cartApi } = await import('../apiClient');

    await expect(cartApi.getCart()).rejects.toMatchObject({ kind: 'network' });
  });
});