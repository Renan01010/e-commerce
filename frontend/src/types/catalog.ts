export interface Product {
  id: string;
  name: string;
  description: string | null;
  price: number;
  cost: number | null;
  brand: string | null;
  sku: string;
  categoryId: string;
  quantity: number;
  imageUrl: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  name: string;
  description: string | null;
  parentCategoryId: string | null;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductPage {
  content: Product[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasMore: boolean;
}

export interface CatalogFilters {
  categoryId?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  brand?: string;
}

export type ProductSort = 'relevance' | 'price' | 'name' | 'newest';
export type SortOrder = 'asc' | 'desc';