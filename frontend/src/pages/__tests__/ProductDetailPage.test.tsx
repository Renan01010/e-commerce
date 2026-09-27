import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { catalogApi } from '../../services/apiClient';
import { useCatalogStore } from '../../store/catalogStore';
import { ProductDetailPage } from '../ProductDetailPage';

vi.mock('../../services/apiClient', () => ({
  catalogApi: { getProducts: vi.fn(), getCategories: vi.fn(), getProduct: vi.fn() },
  getApiErrorMessage: () => 'Produto indisponível',
}));

describe('ProductDetailPage', () => {
  beforeEach(() => {
    useCatalogStore.setState({ selectedProduct: null, categories: [], isLoading: false, error: null });
    vi.mocked(catalogApi.getCategories).mockResolvedValue([]);
  });

  it('loads details and keeps cart as a non-functional placeholder', async () => {
    vi.mocked(catalogApi.getProduct).mockResolvedValue({
      id: 'p-1', name: 'Fone Studio', description: 'Som sem ruído', price: 399.9, cost: null,
      brand: 'Acme', sku: 'AC-1', categoryId: 'c-1', quantity: 3, imageUrl: null,
      isActive: true, createdAt: '', updatedAt: '',
    });
    render(<MemoryRouter initialEntries={['/products/p-1']}><Routes>
      <Route path="/products/:id" element={<ProductDetailPage />} />
    </Routes></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Fone Studio' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Adicionar ao carrinho' })).toBeDisabled();
    expect(screen.getByText('Carrinho indisponível neste momento.')).toBeInTheDocument();
  });
});