import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { catalogApi } from '../../services/apiClient';
import { useCatalogStore } from '../../store/catalogStore';
import type { Category, Product } from '../../types/catalog';
import { HomePage } from '../HomePage';

vi.mock('../../services/apiClient', () => ({
  catalogApi: { getProducts: vi.fn(), getCategories: vi.fn(), getProduct: vi.fn() },
  getApiErrorMessage: () => 'Erro do catálogo',
}));

const product: Product = {
  id: 'product-1', name: 'Monitor de estudo', description: 'Tela para o dia a dia', price: 899.9,
  cost: null, brand: 'Acme', sku: 'MON-1', categoryId: 'category-1', quantity: 4,
  imageUrl: null, isActive: true, createdAt: '', updatedAt: '',
};

const categories: Category[] = [
  { id: 'category-2', name: 'Acessórios', description: null, parentCategoryId: null, displayOrder: 2, isActive: true, createdAt: '', updatedAt: '' },
  { id: 'category-1', name: 'Monitores', description: null, parentCategoryId: null, displayOrder: 1, isActive: true, createdAt: '', updatedAt: '' },
];

function LocationOutput() {
  const location = useLocation();
  return <output aria-label="Localização">{location.pathname}{location.search}</output>;
}

describe('HomePage', () => {
  beforeEach(() => {
    useCatalogStore.setState({
      recentProducts: [], recentProductsLoading: false, recentProductsStatus: 'idle', recentProductsError: null,
      categories: [], categoriesStatus: 'idle', categoriesError: null,
    });
    vi.clearAllMocks();
    vi.mocked(catalogApi.getProducts).mockResolvedValue({
      content: [product], totalElements: 1, totalPages: 1, currentPage: 0, pageSize: 8, hasMore: false,
    });
    vi.mocked(catalogApi.getCategories).mockResolvedValue(categories);
  });

  it('renders recent API products and real categories in display order', async () => {
    render(<MemoryRouter><HomePage /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: 'Monitor de estudo' })).toBeInTheDocument();
    const categoryLinks = await screen.findAllByRole('link', { name: /Monitores|Acessórios/ });
    expect(categoryLinks[0]).toHaveAttribute('href', '/catalog?categoryId=category-1');
    expect(categoryLinks[1]).toHaveAttribute('href', '/catalog?categoryId=category-2');
    expect(screen.getByRole('link', { name: 'Ver Monitor de estudo' })).toHaveAttribute('href', '/products/product-1');
    expect(catalogApi.getProducts).toHaveBeenCalledWith({
      query: '', filters: {}, sortBy: 'newest', sortOrder: 'desc', page: 0, pageSize: 8,
    });
  });

  it('announces independent category and product loading skeletons', async () => {
    let resolveProducts: ((value: { content: Product[]; totalElements: number; totalPages: number; currentPage: number; pageSize: number; hasMore: boolean }) => void) | undefined;
    let resolveCategories: ((value: Category[]) => void) | undefined;
    vi.mocked(catalogApi.getProducts).mockReturnValue(new Promise((resolve) => { resolveProducts = resolve; }));
    vi.mocked(catalogApi.getCategories).mockReturnValue(new Promise((resolve) => { resolveCategories = resolve; }));
    render(<MemoryRouter><HomePage /></MemoryRouter>);

    expect(screen.getByRole('status', { name: 'Carregando produtos' })).toBeInTheDocument();
    expect(screen.getByRole('status', { name: 'Carregando categorias' })).toBeInTheDocument();
    resolveProducts?.({ content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 8, hasMore: false });
    resolveCategories?.([]);
    expect(await screen.findByText('Nenhum produto foi encontrado no catálogo.')).toBeInTheDocument();
    expect(await screen.findByText('Nenhuma categoria disponível no momento.')).toBeInTheDocument();
  });

  it('shows a real empty-catalog message when the API returns no products', async () => {
    vi.mocked(catalogApi.getProducts).mockResolvedValue({
      content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 8, hasMore: false,
    });
    render(<MemoryRouter><HomePage /></MemoryRouter>);

    expect(await screen.findByText('Nenhum produto foi encontrado no catálogo.')).toBeInTheDocument();
  });

  it('adapts the category section to a small API result and keeps the catalog link', async () => {
    vi.mocked(catalogApi.getCategories).mockResolvedValue([categories[0]]);
    render(<MemoryRouter><HomePage /></MemoryRouter>);

    const categorySection = screen.getByRole('region', { name: 'Explore por categoria' });
    expect(await within(categorySection).findByRole('link', { name: /Acessórios/ })).toHaveAttribute(
      'href',
      '/catalog?categoryId=category-2',
    );
    expect(within(categorySection).getAllByRole('link', { name: 'Ver catálogo' })
      .some((link) => link.getAttribute('href') === '/catalog')).toBe(true);
    expect(within(categorySection).queryByRole('link', { name: /Monitores/ })).not.toBeInTheDocument();
  });

  it('keeps a catalog link visible when the API returns no categories', async () => {
    vi.mocked(catalogApi.getCategories).mockResolvedValue([]);
    render(<MemoryRouter><HomePage /></MemoryRouter>);

    const categorySection = screen.getByRole('region', { name: 'Explore por categoria' });
    expect(await within(categorySection).findByText('Nenhuma categoria disponível no momento.')).toBeInTheDocument();
    expect(within(categorySection).getAllByRole('link', { name: 'Ver catálogo' })
      .some((link) => link.getAttribute('href') === '/catalog')).toBe(true);
    expect(within(categorySection).queryByRole('link', { name: /Notebooks|Monitores|PC Gamer/ })).not.toBeInTheDocument();
  });

  it('keeps the category section available when recent-products loading fails', async () => {
    vi.mocked(catalogApi.getProducts).mockRejectedValue(new Error('internal trace'));
    render(<MemoryRouter><HomePage /></MemoryRouter>);

    expect(await screen.findByRole('link', { name: /Monitores/ })).toBeInTheDocument();
    expect(await screen.findByText('Erro do catálogo')).toBeInTheDocument();
    expect(screen.queryByText('internal trace')).not.toBeInTheDocument();
  });

  it('retries category loading without showing invented categories', async () => {
    const user = userEvent.setup();
    vi.mocked(catalogApi.getCategories).mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce([categories[0]]);
    render(<MemoryRouter><HomePage /></MemoryRouter>);

    expect(await screen.findByText('Erro do catálogo')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    expect(await screen.findByRole('link', { name: /Acessórios/ })).toHaveAttribute('href', '/catalog?categoryId=category-2');
    expect(catalogApi.getCategories).toHaveBeenCalledTimes(2);
  });

  it('submits a search to the catalog URL without per-keystroke navigation', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/catalog" element={<LocationOutput />} />
        </Routes>
      </MemoryRouter>,
    );

    await user.type(screen.getByRole('searchbox', { name: 'Buscar produtos no catálogo' }), 'fone & teclado');
    expect(screen.queryByLabelText('Localização')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Buscar' }));
    expect(screen.getByLabelText('Localização')).toHaveTextContent('/catalog?query=fone%20%26%20teclado');
  });
});
