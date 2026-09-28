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