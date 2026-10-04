import { create } from 'zustand';
import { catalogApi, getApiErrorMessage } from '../services/apiClient';
import type { CatalogFilters, Category, Product, ProductPage, ProductSort, SortOrder } from '../types/catalog';

interface CatalogState {
  products: Product[];
  recentProducts: Product[];
  recentProductsLoading: boolean;
  recentProductsStatus: 'idle' | 'loading' | 'loaded' | 'error';
  recentProductsError: string | null;
  categories: Category[];
  categoriesStatus: 'idle' | 'loading' | 'loaded' | 'error';
  selectedProduct: Product | null;
  searchQuery: string;
  filters: CatalogFilters;
  sortBy: ProductSort;
  sortOrder: SortOrder;
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasMore: boolean;
  isLoading: boolean;
  error: string | null;
  categoriesError: string | null;
  setSearchQuery: (searchQuery: string) => void;
  setUrlCriteria: (searchQuery: string, categoryId?: string) => void;
  setFilters: (filters: Partial<CatalogFilters>) => void;
  clearFilters: () => void;
  setSort: (sortBy: ProductSort, sortOrder: SortOrder) => void;
  setPage: (page: number) => void;
  loadProducts: () => Promise<void>;
  loadRecentProducts: (retry?: boolean) => Promise<void>;
  loadCategories: (retry?: boolean) => Promise<void>;
  loadProduct: (id: string) => Promise<Product | null>;
  resetCatalog: () => void;
}

const emptyPage: ProductPage = {
  content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 20, hasMore: false,
};

let productsRequestId = 0;
let recentProductsRequestId = 0;
let categoriesRequestId = 0;
let productRequestId = 0;
let pendingRecentProducts: Promise<void> | null = null;
let pendingCategories: Promise<void> | null = null;

export const useCatalogStore = create<CatalogState>((set, get) => ({
  products: [],
  recentProducts: [],
  recentProductsLoading: false,
  recentProductsStatus: 'idle',
  recentProductsError: null,
  categories: [],
  categoriesStatus: 'idle',
  selectedProduct: null,
  searchQuery: '',
  filters: {},
  sortBy: 'relevance',
  sortOrder: 'desc',
  currentPage: 0,
  pageSize: 20,
  totalElements: 0,
  totalPages: 0,
  hasMore: false,
  isLoading: false,
  error: null,
  categoriesError: null,
  setSearchQuery: (searchQuery) => set({ searchQuery, currentPage: 0 }),
  setUrlCriteria: (searchQuery, categoryId) => set((state) => {
    const filters = { ...state.filters, categoryId };
    if (state.searchQuery === searchQuery && state.filters.categoryId === categoryId) return state;
    return { searchQuery, filters, currentPage: 0 };
  }),
  setFilters: (filters) => set((state) => ({ filters: { ...state.filters, ...filters }, currentPage: 0 })),
  clearFilters: () => set({ filters: {}, currentPage: 0 }),
  setSort: (sortBy, sortOrder) => set({ sortBy, sortOrder, currentPage: 0 }),
  setPage: (currentPage) => set({ currentPage }),
  loadProducts: async () => {
    const requestId = ++productsRequestId;
    set({ isLoading: true, error: null });
    try {
      const requestState = get();
      const page = await catalogApi.getProducts({
        query: requestState.searchQuery,
        filters: requestState.filters,
        sortBy: requestState.sortBy,
        sortOrder: requestState.sortOrder,
        page: requestState.currentPage,
        pageSize: requestState.pageSize,
      });
      if (requestId !== productsRequestId) return;
      const currentState = get();
      set({ products: page.content ?? [], totalElements: page.totalElements ?? 0, totalPages: page.totalPages ?? 0,
        currentPage: page.currentPage ?? currentState.currentPage, pageSize: page.pageSize ?? currentState.pageSize,
        hasMore: page.hasMore ?? false, isLoading: false });
    } catch (error) {
      if (requestId !== productsRequestId) return;
      set({ ...emptyPage, error: getApiErrorMessage(error), isLoading: false });
    }
  },
  loadRecentProducts: (retry = false) => {
    const state = get();
    if (state.recentProductsLoading && pendingRecentProducts && !retry) return pendingRecentProducts;
    if (state.recentProductsStatus === 'loaded' && !retry) return Promise.resolve();

    const requestId = ++recentProductsRequestId;
    set({ recentProductsLoading: true, recentProductsStatus: 'loading', recentProductsError: null });
    const promise = Promise.resolve().then(async () => {
      try {
        const page = await catalogApi.getProducts({
          query: '',
          filters: {},
          sortBy: 'newest',
          sortOrder: 'desc',
          page: 0,
          pageSize: 8,
        });
        if (requestId === recentProductsRequestId) {
          set({ recentProducts: page.content ?? [], recentProductsLoading: false, recentProductsStatus: 'loaded' });
        }
      } catch (error) {
        if (requestId === recentProductsRequestId) {
          set({ recentProductsError: getApiErrorMessage(error), recentProductsLoading: false, recentProductsStatus: 'error' });
        }
      } finally {
        if (pendingRecentProducts === promise) pendingRecentProducts = null;
      }
    });
    pendingRecentProducts = promise;
    return promise;
  },
  loadCategories: (retry = false) => {
    const state = get();
    if (state.categoriesStatus === 'loading' && pendingCategories && !retry) return pendingCategories;
    if (state.categoriesStatus === 'loaded' && !retry) return Promise.resolve();

    const requestId = ++categoriesRequestId;
    set({ categoriesError: null, categoriesStatus: 'loading' });
    const promise = Promise.resolve().then(async () => {
      try {
        const categories = await catalogApi.getCategories();
        if (requestId === categoriesRequestId) set({ categories, categoriesStatus: 'loaded' });
      } catch (error) {
        if (requestId === categoriesRequestId) {
          set({ categoriesError: getApiErrorMessage(error), categoriesStatus: 'error' });
        }
      } finally {
        if (pendingCategories === promise) pendingCategories = null;
      }
    });
    pendingCategories = promise;
    return promise;
  },
  loadProduct: async (id) => {
    const requestId = ++productRequestId;
    set({ isLoading: true, error: null, selectedProduct: null });
    try {
      const selectedProduct = await catalogApi.getProduct(id);
      if (requestId !== productRequestId) return null;
      set({ selectedProduct, isLoading: false });
      return selectedProduct;
    } catch (error) {
      if (requestId !== productRequestId) return null;
      set({ error: getApiErrorMessage(error), isLoading: false });
      return null;
    }
  },
  resetCatalog: () => {
    productsRequestId += 1;
    recentProductsRequestId += 1;
    categoriesRequestId += 1;
    productRequestId += 1;
    pendingRecentProducts = null;
    pendingCategories = null;
    set({
      products: [],
      recentProducts: [],
      recentProductsLoading: false,
      recentProductsStatus: 'idle',
      recentProductsError: null,
      categories: [],
      categoriesStatus: 'idle',
      selectedProduct: null,
      searchQuery: '',
      filters: {},
      sortBy: 'relevance',
      sortOrder: 'desc',
      currentPage: 0,
      pageSize: 20,
      totalElements: 0,
      totalPages: 0,
      hasMore: false,
      isLoading: false,
      error: null,
      categoriesError: null,
    });
  },
}));