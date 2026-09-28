import axios from 'axios';
import { useAuthStore } from '../store/authStore';
import type { AddCartItemRequest, CartResponse } from '../types/cart';
import type { CatalogFilters, Category, Product, ProductPage, ProductSort, SortOrder } from '../types/catalog';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT ?? 10000),
});

apiClient.interceptors.request.use((config) => {
  if (config.url !== '/auth/login') {
    const session = useAuthStore.getState().getValidSession();
    if (session) config.headers.set('Authorization', `${session.tokenType} ${session.accessToken}`);
  }
  return config;
});

export interface ProductQuery {
  query: string;
  filters: CatalogFilters;
  sortBy: ProductSort;
  sortOrder: SortOrder;
  page: number;
  pageSize: number;
}

export const catalogApi = {
  async getProducts(input: ProductQuery): Promise<ProductPage> {
    const { data } = await apiClient.get<ProductPage>('/products', {
      params: {
        query: input.query || undefined,
        categoryId: input.filters.categoryId,
        minPrice: input.filters.minPrice,
        maxPrice: input.filters.maxPrice,
        inStock: input.filters.inStock,
        brand: input.filters.brand || undefined,
        sortBy: input.sortBy,
        sortOrder: input.sortOrder,
        page: input.page,
        pageSize: input.pageSize,
      },
    });
    if (!data || typeof data !== 'object' || !Array.isArray(data.content)) {
      throw new Error('Resposta inválida do catálogo');
    }
    return data;
  },

  async getProduct(id: string): Promise<Product> {
    const { data } = await apiClient.get<Product>(`/products/${id}`);
    if (!data || typeof data !== 'object' || Array.isArray(data)) {
      throw new Error('Resposta inválida do produto');
    }
    return data;
  },

  async getCategories(): Promise<Category[]> {
    const { data } = await apiClient.get<Category[]>('/categories');
    if (!Array.isArray(data)) {
      throw new Error('Resposta inválida das categorias');
    }
    return data;
  },
};

export type CartApiErrorKind = 'unauthenticated' | 'not-found' | 'unavailable' | 'network' | 'unknown';

export class CartApiError extends Error {
  constructor(
    message: string,
    readonly kind: CartApiErrorKind,
    readonly status?: number,
  ) {
    super(message);
    this.name = 'CartApiError';
  }
}

function toCartApiError(error: unknown): CartApiError {
  if (!axios.isAxiosError(error)) {
    return new CartApiError('O carrinho não pôde ser atualizado.', 'unknown');
  }

  const status = error.response?.status;
  if (status === 401) {
    return new CartApiError('Entre na sua conta para acessar o carrinho.', 'unauthenticated', status);
  }
  if (status === 404) {
    return new CartApiError('Este item não está mais disponível no carrinho.', 'not-found', status);
  }
  if (status === 503) {
    return new CartApiError('O carrinho está temporariamente indisponível. Tente novamente.', 'unavailable', status);
  }
  if (!error.response) {
    return new CartApiError('Não foi possível conectar ao carrinho. Tente novamente.', 'network');
  }
  return new CartApiError('O carrinho não pôde ser atualizado. Tente novamente.', 'unknown', status);
}

async function requestCart<T>(request: () => Promise<{ data: T }>): Promise<T> {
  try {
    const response = await request();
    return response.data;
  } catch (error) {
    throw toCartApiError(error);
  }
}

export const cartApi = {
  getCart(): Promise<CartResponse> {
    return requestCart(() => apiClient.get<CartResponse>('/cart'));
  },

  addItem(body: AddCartItemRequest): Promise<CartResponse> {
    return requestCart(() => apiClient.post<CartResponse>('/cart/items', body));
  },

  setQuantity(productId: string, quantity: number): Promise<CartResponse> {
    return requestCart(() => apiClient.put<CartResponse>(`/cart/items/${encodeURIComponent(productId)}`, { quantity }));
  },

  removeItem(productId: string): Promise<void> {
    return requestCart(() => apiClient.delete<void>(`/cart/items/${encodeURIComponent(productId)}`));
  },

  clearCart(): Promise<void> {
    return requestCart(() => apiClient.delete<void>('/cart'));
  },
};

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) return 'Não foi possível conectar ao catálogo. Tente novamente.';
    const message = (error.response.data as { message?: string } | undefined)?.message;
    return message ?? 'O catálogo não pôde ser carregado.';
  }
  return 'O catálogo não pôde ser carregado.';
}