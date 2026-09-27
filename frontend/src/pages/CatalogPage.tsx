import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ProductCard } from '../components/ProductCard';
import { useCatalogStore } from '../store/catalogStore';
import type { ProductSort, SortOrder } from '../types/catalog';

function categoryLabel(categoryId: string, categories: ReturnType<typeof useCatalogStore.getState>['categories'], seen = new Set<string>()): string {
  const category = categories.find((item) => item.id === categoryId);
  if (!category || seen.has(category.id)) return '';
  seen.add(category.id);
  const parent = category.parentCategoryId ? categoryLabel(category.parentCategoryId, categories, seen) : '';
  return [parent, category.name].filter(Boolean).join(' / ');
}

export function CatalogPage() {
  const [searchParams] = useSearchParams();
  const categoryFromUrl = searchParams.get('categoryId');
  const [searchInput, setSearchInput] = useState(useCatalogStore.getState().searchQuery);
  const products = useCatalogStore((state) => state.products);
  const categories = useCatalogStore((state) => state.categories);
  const filters = useCatalogStore((state) => state.filters);
  const sortBy = useCatalogStore((state) => state.sortBy);
  const sortOrder = useCatalogStore((state) => state.sortOrder);
  const currentPage = useCatalogStore((state) => state.currentPage);
  const totalPages = useCatalogStore((state) => state.totalPages);
  const totalElements = useCatalogStore((state) => state.totalElements);
  const hasMore = useCatalogStore((state) => state.hasMore);
  const isLoading = useCatalogStore((state) => state.isLoading);
  const error = useCatalogStore((state) => state.error);

  useEffect(() => {
    const timer = window.setTimeout(() => useCatalogStore.getState().setSearchQuery(searchInput), 250);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    void useCatalogStore.getState().loadCategories();
  }, []);

  useEffect(() => {
    if (categoryFromUrl) useCatalogStore.getState().setFilters({ categoryId: categoryFromUrl });
  }, [categoryFromUrl]);

  useEffect(() => {
    void useCatalogStore.getState().loadProducts();
  }, [filters, sortBy, sortOrder, currentPage, useCatalogStore.getState().searchQuery]);

  const setFilter = useCatalogStore((state) => state.setFilters);
  const clearFilters = useCatalogStore((state) => state.clearFilters);
  const setSort = useCatalogStore((state) => state.setSort);
  const setPage = useCatalogStore((state) => state.setPage);
  const searchQuery = useCatalogStore((state) => state.searchQuery);

  const handleSort = (value: string) => {
    const [nextSort, nextOrder] = value.split('-') as [ProductSort, SortOrder];
    setSort(nextSort, nextOrder);
  };

  return (
    <main className="catalog-shell">
      <section className="catalog-heading">
        <div>
          <p className="eyebrow">TECHSTORE / CATÁLOGO</p>
          <h1>Encontre seu próximo favorito.</h1>
        </div>
        <p className="catalog-count">{totalElements.toLocaleString('pt-BR')} produtos</p>
      </section>

      <div className="catalog-layout">
        <aside className="filters" aria-label="Filtros do catálogo">
          <div className="filters__heading">
            <h2>Refinar</h2>
            <button type="button" className="text-button" onClick={clearFilters}>Limpar</button>
          </div>
          <label className="filter-label" htmlFor="category-filter">Categoria</label>
          <select id="category-filter" value={filters.categoryId ?? ''}
            onChange={(event) => setFilter({ categoryId: event.target.value || undefined })}>
            <option value="">Todas as categorias</option>
            {categories.map((category) => <option key={category.id} value={category.id}>{categoryLabel(category.id, categories)}</option>)}
          </select>

          <fieldset className="filter-group">
            <legend>Faixa de preço</legend>
            <div className="price-fields">
              <label><span>Mín.</span><input aria-label="Preço mínimo" type="number" min="0" step="0.01"
                value={filters.minPrice ?? ''} onChange={(event) => setFilter({ minPrice: event.target.value ? Number(event.target.value) : undefined })} /></label>
              <label><span>Máx.</span><input aria-label="Preço máximo" type="number" min="0" step="0.01"
                value={filters.maxPrice ?? ''} onChange={(event) => setFilter({ maxPrice: event.target.value ? Number(event.target.value) : undefined })} /></label>
            </div>
          </fieldset>

          <label className="filter-label" htmlFor="brand-filter">Marca</label>
          <input id="brand-filter" type="search" placeholder="Ex.: Samsung" value={filters.brand ?? ''}
            onChange={(event) => setFilter({ brand: event.target.value || undefined })} />

          <label className="check-row">
            <input type="checkbox" checked={filters.inStock ?? false}
              onChange={(event) => setFilter({ inStock: event.target.checked ? true : undefined })} />
            <span>Somente disponíveis</span>
          </label>
        </aside>

        <section className="catalog-results" aria-label="Produtos">
          <div className="catalog-toolbar">
            <label className="search-box">
              <span className="search-box__icon" aria-hidden="true">⌕</span>
              <input type="search" aria-label="Buscar produtos" placeholder="Busque por produto, marca ou descrição"
                value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
              {searchInput && <button type="button" aria-label="Limpar busca" onClick={() => setSearchInput('')}>×</button>}
            </label>
            <label className="sort-control">Ordenar
              <select aria-label="Ordenar produtos" value={`${sortBy}-${sortOrder}`} onChange={(event) => handleSort(event.target.value)}>
                <option value="relevance-desc">Relevância</option>
                <option value="price-asc">Menor preço</option>
                <option value="price-desc">Maior preço</option>
                <option value="name-asc">Nome A–Z</option>
                <option value="newest-desc">Mais recentes</option>
              </select>
            </label>
          </div>

          {searchQuery && <p className="search-summary">Resultados para <strong>“{searchQuery}”</strong></p>}
          {error && <div className="notice notice--error" role="alert">{error}</div>}
          {isLoading ? (
            <div className="loading-grid" aria-label="Carregando produtos">
              {Array.from({ length: 6 }, (_, index) => <div className="skeleton" key={index} />)}
            </div>
          ) : products.length ? (
            <div className="product-grid">{products.map((product) => <ProductCard key={product.id} product={product} />)}</div>
          ) : !error ? (
            <div className="empty-state">
              <p className="eyebrow">NENHUM RESULTADO</p>
              <h2>Não encontramos produtos por aqui.</h2>
              <p>Tente ajustar a busca ou remover alguns filtros.</p>
              <button type="button" className="button button--outline" onClick={() => { setSearchInput(''); clearFilters(); }}>Limpar busca e filtros</button>
            </div>
          ) : null}

          {totalPages > 1 && (
            <nav className="pagination" aria-label="Paginação">
              <button type="button" className="button button--outline" disabled={currentPage === 0}
                onClick={() => setPage(currentPage - 1)}>Anterior</button>
              <span>Página {currentPage + 1} de {totalPages}</span>
              <button type="button" className="button button--outline" disabled={!hasMore}
                onClick={() => setPage(currentPage + 1)}>Próxima</button>
            </nav>
          )}
        </section>
      </div>
      <footer className="catalog-footer"><span>TECHSTORE</span><Link to="/">Voltar ao início ↑</Link></footer>
    </main>
  );
}