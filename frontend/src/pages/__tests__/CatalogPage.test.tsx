import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom';
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

function LocationOutput() {
  const location = useLocation();
  return <output aria-label="Localização">{location.pathname}{location.search}</output>;
}

function HistoryControls() {
  const navigate = useNavigate();
  return <><button type="button" onClick={() => navigate(-1)}>Voltar histórico</button><button type="button" onClick={() => navigate(1)}>Avançar histórico</button></>;
}

describe('CatalogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useCatalogStore.setState({ products: [], categories: [], selectedProduct: null, searchQuery: '',
      categoriesStatus: 'idle', categoriesError: null,
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

  it('shows an empty state after a search returns no matching products', async () => {
    vi.mocked(catalogApi.getProducts).mockResolvedValue({ content: [], totalElements: 0,
      totalPages: 0, currentPage: 0, pageSize: 20, hasMore: false });
    render(<MemoryRouter initialEntries={['/catalog?query=produto-inexistente']}><CatalogPage /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Não encontramos produtos por aqui.' })).toBeInTheDocument();
  });

  it('retries a catalog error without clearing URL criteria', async () => {
    const user = userEvent.setup();
    vi.mocked(catalogApi.getProducts)
      .mockRejectedValueOnce(new Error('internal trace'))
      .mockResolvedValueOnce({ content: [product], totalElements: 1, totalPages: 1,
        currentPage: 0, pageSize: 20, hasMore: false });
    render(<MemoryRouter initialEntries={['/catalog?query=fone']}><CatalogPage /></MemoryRouter>);

    expect(await screen.findByRole('alert')).toHaveTextContent('Erro do catálogo');
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    expect(await screen.findByRole('heading', { name: 'Fone Studio' })).toBeInTheDocument();
    expect(useCatalogStore.getState().searchQuery).toBe('fone');
    expect(catalogApi.getProducts).toHaveBeenCalledTimes(2);
  });

  it('hydrates query and category from the URL and clears stale URL-controlled filters', async () => {
    useCatalogStore.setState({ currentPage: 3 });
    render(<MemoryRouter initialEntries={['/catalog?query=monitor&categoryId=category-1']}><CatalogPage /></MemoryRouter>);

    await waitFor(() => expect(catalogApi.getProducts).toHaveBeenCalledWith(expect.objectContaining({
      query: 'monitor',
      filters: expect.objectContaining({ categoryId: 'category-1' }),
    })));
    expect(screen.getByLabelText('Buscar produtos')).toHaveValue('monitor');
    expect(useCatalogStore.getState().currentPage).toBe(0);
  });

  it('clears stale query and category from state when URL parameters are absent', async () => {
    useCatalogStore.setState({ searchQuery: 'stale query', filters: { categoryId: 'stale-category', brand: 'Acme' } });
    render(<MemoryRouter initialEntries={['/catalog']}><CatalogPage /></MemoryRouter>);

    await waitFor(() => expect(catalogApi.getProducts).toHaveBeenCalledWith(expect.objectContaining({
      query: '',
      filters: expect.objectContaining({ categoryId: undefined, brand: 'Acme' }),
    })));
    expect(screen.getByLabelText('Buscar produtos')).toHaveValue('');
  });

  it('updates query and category URL values when their controls change', async () => {
    const user = userEvent.setup();
    useCatalogStore.setState({ categories: [{
      id: 'category-1', name: 'Monitores', description: null, parentCategoryId: null,
      displayOrder: 1, isActive: true, createdAt: '', updatedAt: '',
    }], categoriesStatus: 'loaded' });
    render(
      <MemoryRouter initialEntries={['/catalog']}>
        <CatalogPage />
        <LocationOutput />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('Buscar produtos'), 'monitor');
    await waitFor(() => expect(screen.getByLabelText('Localização')).toHaveTextContent('/catalog?query=monitor'));
    await user.selectOptions(screen.getByLabelText('Categoria'), 'category-1');
    expect(screen.getByLabelText('Localização')).toHaveTextContent('/catalog?query=monitor&categoryId=category-1');
  });

  it('restores query and category from browser history navigation', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/catalog?query=teclado', '/catalog?query=monitor&categoryId=category-1']}>
        <CatalogPage />
        <LocationOutput />
        <HistoryControls />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: 'Voltar histórico' }));
    await waitFor(() => expect(screen.getByLabelText('Buscar produtos')).toHaveValue('teclado'));
    expect(screen.getByLabelText('Localização')).toHaveTextContent('/catalog?query=teclado');
    expect(useCatalogStore.getState().filters.categoryId).toBeUndefined();

    await user.click(screen.getByRole('button', { name: 'Avançar histórico' }));
    await waitFor(() => expect(screen.getByLabelText('Buscar produtos')).toHaveValue('monitor'));
    expect(useCatalogStore.getState().filters.categoryId).toBe('category-1');
  });
});