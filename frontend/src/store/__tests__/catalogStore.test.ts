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
  products: [], recentProducts: [], recentProductsLoading: false, recentProductsStatus: 'idle', recentProductsError: null,
  categories: [], categoriesStatus: 'idle', selectedProduct: null, searchQuery: '', filters: {},
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

  it('loads and caches the eight newest products independently, including an empty response', async () => {
    useCatalogStore.setState({ products: [product] });
    vi.mocked(catalogApi.getProducts).mockResolvedValue({ content: [], totalElements: 0, totalPages: 0,
      currentPage: 0, pageSize: 8, hasMore: false });

    await useCatalogStore.getState().loadRecentProducts();
    await useCatalogStore.getState().loadRecentProducts();

    expect(catalogApi.getProducts).toHaveBeenCalledTimes(1);
    expect(catalogApi.getProducts).toHaveBeenCalledWith({
      query: '', filters: {}, sortBy: 'newest', sortOrder: 'desc', page: 0, pageSize: 8,
    });
    expect(useCatalogStore.getState().recentProductsStatus).toBe('loaded');
    expect(useCatalogStore.getState().products).toEqual([product]);
  });

  it('deduplicates category requests and caches a successful response', async () => {
    const firstRequest = useCatalogStore.getState().loadCategories();
    expect(useCatalogStore.getState().categoriesStatus).toBe('loading');
    const secondRequest = useCatalogStore.getState().loadCategories();
    await Promise.all([firstRequest, secondRequest]);
    await useCatalogStore.getState().loadCategories();

    expect(catalogApi.getCategories).toHaveBeenCalledTimes(1);
    expect(useCatalogStore.getState().categoriesStatus).toBe('loaded');
  });

  it('retries failed recent products and category requests', async () => {
    vi.mocked(catalogApi.getProducts)
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce({ content: [product], totalElements: 1, totalPages: 1,
        currentPage: 0, pageSize: 8, hasMore: false });
    vi.mocked(catalogApi.getCategories).mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce([]);

    await useCatalogStore.getState().loadRecentProducts();
    await useCatalogStore.getState().loadCategories();
    expect(useCatalogStore.getState().recentProductsStatus).toBe('error');
    expect(useCatalogStore.getState().categoriesStatus).toBe('error');

    await useCatalogStore.getState().loadRecentProducts(true);
    await useCatalogStore.getState().loadCategories(true);
    expect(useCatalogStore.getState().recentProducts).toEqual([product]);
    expect(useCatalogStore.getState().recentProductsStatus).toBe('loaded');
    expect(useCatalogStore.getState().categoriesStatus).toBe('loaded');
  });

  it('ignores obsolete responses from retried recent-products requests', async () => {
    let resolveFirst: ((value: unknown) => void) | undefined;
    const first = new Promise((resolve) => { resolveFirst = resolve; });
    vi.mocked(catalogApi.getProducts)
      .mockReturnValueOnce(first as ReturnType<typeof catalogApi.getProducts>)
      .mockResolvedValueOnce({ content: [product], totalElements: 1, totalPages: 1,
        currentPage: 0, pageSize: 8, hasMore: false });

    const firstRequest = useCatalogStore.getState().loadRecentProducts();
    const retryRequest = useCatalogStore.getState().loadRecentProducts(true);
    await retryRequest;
    resolveFirst?.({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 8, hasMore: false });
    await firstRequest;

    expect(useCatalogStore.getState().recentProducts).toEqual([product]);
  });

  it('ignores obsolete responses from retried category requests', async () => {
    const category = {
      id: 'category-1', name: 'Monitores', description: null, parentCategoryId: null,
      displayOrder: 1, isActive: true, createdAt: '', updatedAt: '',
    };
    let resolveFirst: ((value: unknown) => void) | undefined;
    const first = new Promise((resolve) => { resolveFirst = resolve; });
    vi.mocked(catalogApi.getCategories)
      .mockReturnValueOnce(first as ReturnType<typeof catalogApi.getCategories>)
      .mockResolvedValueOnce([]);

    const firstRequest = useCatalogStore.getState().loadCategories();
    const retryRequest = useCatalogStore.getState().loadCategories(true);
    await retryRequest;
    resolveFirst?.([category]);
    await firstRequest;

    expect(useCatalogStore.getState().categories).toEqual([]);
    expect(useCatalogStore.getState().categoriesStatus).toBe('loaded');
  });
});
