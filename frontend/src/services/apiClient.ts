import axios from 'axios';
import { useAuthStore } from '../store/authStore';
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
    return data;
  },

  async getProduct(id: string): Promise<Product> {
    const { data } = await apiClient.get<Product>(`/products/${id}`);
    return data;
  },

  async getCategories(): Promise<Category[]> {
    const { data } = await apiClient.get<Category[]>('/categories');
    return data;
  },
};

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) return 'Não foi possível conectar ao catálogo. Tente novamente.';
    const message = (error.response.data as { message?: string } | undefined)?.message;
    return message ?? 'O catálogo não pôde ser carregado.';
  }
  return 'Ocorreu um erro inesperado.';
}