import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { catalogApi } from '../../services/apiClient';
import { useCatalogStore } from '../../store/catalogStore';
import { CatalogPage } from '../CatalogPage';

vi.mock('../../services/apiClient', () => ({
  catalogApi: { getProducts: vi.fn(), getCategories: vi.fn(), getProduct: vi.fn() },
  getApiErrorMessage: () => 'Erro do catálogo',
}));

const product = {
  id: 'p-1', name: 'Fone Studio', description: 'Som sem ruído', price: 399.9, cost: null,
  brand: 'Acme', sku: 'AC-1', categoryId: 'c-1', quantity: 3, imageUrl: null,
  isActive: true, createdAt: '', updatedAt: '',
};

describe('CatalogPage', () => {
  beforeEach(() => {
    useCatalogStore.setState({ products: [], categories: [], selectedProduct: null, searchQuery: '',
      filters: {}, sortBy: 'relevance', sortOrder: 'desc', currentPage: 0, pageSize: 20,
      totalElements: 0, totalPages: 0, hasMore: false, isLoading: false, error: null });
    vi.mocked(catalogApi.getProducts).mockResolvedValue({ content: [product], totalElements: 1,
      totalPages: 1, currentPage: 0, pageSize: 20, hasMore: false });
    vi.mocked(catalogApi.getCategories).mockResolvedValue([]);
  });

  it('loads products through the catalog API and renders the listing', async () => {
    render(<MemoryRouter><CatalogPage /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Fone Studio' })).toBeInTheDocument();
    await waitFor(() => expect(catalogApi.getProducts).toHaveBeenCalled());
    expect(screen.getByLabelText('Buscar produtos')).toBeInTheDocument();
    expect(screen.getByLabelText('Ordenar produtos')).toBeInTheDocument();
  });

  it('shows an empty state when there are no matching products', async () => {
    vi.mocked(catalogApi.getProducts).mockResolvedValue({ content: [], totalElements: 0,
      totalPages: 0, currentPage: 0, pageSize: 20, hasMore: false });
    render(<MemoryRouter><CatalogPage /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Não encontramos produtos por aqui.' })).toBeInTheDocument();
  });
});