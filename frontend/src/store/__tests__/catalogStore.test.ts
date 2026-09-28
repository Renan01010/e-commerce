import { beforeEach, describe, expect, it, vi } from 'vitest';
import { catalogApi } from '../../services/apiClient';
import { useCatalogStore } from '../catalogStore';

vi.mock('../../services/apiClient', () => ({
  catalogApi: { getProducts: vi.fn(), getCategories: vi.fn(), getProduct: vi.fn() },
  getApiErrorMessage: () => 'Erro do catálogo',
}));

const product = {
  id: 'p-1', name: 'Fone Studio', description: 'Som sem ruído', price: 399.9, cost: null,
  brand: 'Acme', sku: 'AC-1', categoryId: 'c-1', quantity: 3, imageUrl: null,
  isActive: true, createdAt: '', updatedAt: '',
};

const resetStore = () => useCatalogStore.setState({
  products: [], categories: [], selectedProduct: null, searchQuery: '', filters: {},
  sortBy: 'relevance', sortOrder: 'desc', currentPage: 0, pageSize: 20,
  totalElements: 0, totalPages: 0, hasMore: false, isLoading: false, error: null,
  categoriesError: null,
});

describe('catalogStore', () => {
  beforeEach(() => {
    resetStore();
    vi.clearAllMocks();
  });

  it('resets pagination when query, filters or sorting change', () => {
    useCatalogStore.setState({ currentPage: 3 });

    useCatalogStore.getState().setSearchQuery('fone');
    expect(useCatalogStore.getState().currentPage).toBe(0);

    useCatalogStore.setState({ currentPage: 2 });
    useCatalogStore.getState().setFilters({ brand: 'Acme' });
    expect(useCatalogStore.getState().currentPage).toBe(0);

    useCatalogStore.setState({ currentPage: 2 });
    useCatalogStore.getState().setSort('price', 'asc');
    expect(useCatalogStore.getState().currentPage).toBe(0);
  });

  it('does not allow an older products response to overwrite a newer one', async () => {
    let resolveFirst: ((value: unknown) => void) | undefined;
    const first = new Promise((resolve) => { resolveFirst = resolve; });
    vi.mocked(catalogApi.getProducts)
      .mockReturnValueOnce(first as ReturnType<typeof catalogApi.getProducts>)
      .mockResolvedValueOnce({ content: [product], totalElements: 1, totalPages: 1,
        currentPage: 0, pageSize: 20, hasMore: false });

    const firstRequest = useCatalogStore.getState().loadProducts();
    const secondRequest = useCatalogStore.getState().loadProducts();
    await secondRequest;
    resolveFirst?.({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 20, hasMore: false });
    await firstRequest;

    expect(useCatalogStore.getState().products).toEqual([product]);
  });
});
