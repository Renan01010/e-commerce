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
  setSearchQuery: (searchQuery) => set({ searchQuery, currentPage: 0 }),
  setFilters: (filters) => set((state) => ({ filters: { ...state.filters, ...filters }, currentPage: 0 })),
  clearFilters: () => set({ filters: {}, currentPage: 0 }),
  setSort: (sortBy, sortOrder) => set({ sortBy, sortOrder, currentPage: 0 }),
  setPage: (currentPage) => set({ currentPage }),
  loadProducts: async () => {
    set({ isLoading: true, error: null });
    try {
      const state = get();
      const page = await catalogApi.getProducts({
        query: state.searchQuery,
        filters: state.filters,
        sortBy: state.sortBy,
        sortOrder: state.sortOrder,
        page: state.currentPage,
        pageSize: state.pageSize,
      });
      set({ products: page.content, totalElements: page.totalElements, totalPages: page.totalPages,
        currentPage: page.currentPage, pageSize: page.pageSize, hasMore: page.hasMore, isLoading: false });
    } catch (error) {
      set({ ...emptyPage, error: getApiErrorMessage(error), isLoading: false });
    }
  },
  loadCategories: async () => {
    try {
      set({ categories: await catalogApi.getCategories() });
    } catch (error) {
      set({ error: getApiErrorMessage(error) });
    }
  },
  loadProduct: async (id) => {
    set({ isLoading: true, error: null, selectedProduct: null });
    try {
      const selectedProduct = await catalogApi.getProduct(id);
      set({ selectedProduct, isLoading: false });
      return selectedProduct;
    } catch (error) {
      set({ error: getApiErrorMessage(error), isLoading: false });
      return null;
    }
  },
}));