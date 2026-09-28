import { create } from 'zustand';
import { catalogApi, getApiErrorMessage } from '../services/apiClient';
import type { CatalogFilters, Category, Product, ProductPage, ProductSort, SortOrder } from '../types/catalog';

interface CatalogState {
  products: Product[];
  categories: Category[];
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
  setFilters: (filters: Partial<CatalogFilters>) => void;
  clearFilters: () => void;
  setSort: (sortBy: ProductSort, sortOrder: SortOrder) => void;
  setPage: (page: number) => void;
  loadProducts: () => Promise<void>;
  loadCategories: () => Promise<void>;
  loadProduct: (id: string) => Promise<Product | null>;
}

const emptyPage: ProductPage = {
  content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 20, hasMore: false,
};

let productsRequestId = 0;
let categoriesRequestId = 0;
let productRequestId = 0;

export const useCatalogStore = create<CatalogState>((set, get) => ({
  products: [],
  categories: [],
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
  loadCategories: async () => {
    const requestId = ++categoriesRequestId;
    set({ categoriesError: null });
    try {
      const categories = await catalogApi.getCategories();
      if (requestId === categoriesRequestId) set({ categories });
    } catch (error) {
      if (requestId === categoriesRequestId) set({ categoriesError: getApiErrorMessage(error) });
    }
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
}));