import { useEffect, useState } from 'react';
import { Filter, Search, X } from 'lucide-react';
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
  const [filtersOpen, setFiltersOpen] = useState(false);
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
  const categoriesError = useCatalogStore((state) => state.categoriesError);
  const loadProducts = useCatalogStore((state) => state.loadProducts);
  const loadCategories = useCatalogStore((state) => state.loadCategories);
  const searchQuery = useCatalogStore((state) => state.searchQuery);
  const hasInvalidPriceRange = filters.minPrice !== undefined && filters.maxPrice !== undefined
    && filters.minPrice > filters.maxPrice;
  const productCount = totalElements ?? 0;
  const featuredProduct = products[0];

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
    if (!hasInvalidPriceRange) void loadProducts();
  }, [filters, sortBy, sortOrder, currentPage, loadProducts, hasInvalidPriceRange, searchQuery]);

  const setFilter = useCatalogStore((state) => state.setFilters);
  const clearFilters = useCatalogStore((state) => state.clearFilters);
  const setSort = useCatalogStore((state) => state.setSort);
  const setPage = useCatalogStore((state) => state.setPage);
  const handleSort = (value: string) => {
    const [nextSort, nextOrder] = value.split('-') as [ProductSort, SortOrder];
    setSort(nextSort, nextOrder);
  };

  return (
    <main className="catalog-shell">
      <section className="catalog-hero">
        <div className="catalog-hero__copy">
          <nav className="breadcrumbs" aria-label="Navegação estrutural"><span>TechStore</span><span aria-hidden="true">/</span><strong>Catálogo</strong></nav>
          <p className="eyebrow">TECNOLOGIA PARA O SEU DIA</p>
          <h1>Encontre algo que acompanhe seu ritmo.</h1>
          <p className="catalog-hero__subtitle">Peças inteligentes, design preciso e tudo para deixar sua rotina mais interessante.</p>
        </div>
        <div className="catalog-hero__feature" aria-label="Destaque do catálogo">
          {featuredProduct?.imageUrl ? <img src={featuredProduct.imageUrl} alt="" /> : <div className="catalog-hero__feature-empty">TECHSTORE<br /><span>CATÁLOGO</span></div>}
          {featuredProduct && <div className="catalog-hero__feature-copy"><p className="eyebrow">EM DESTAQUE</p><strong>{featuredProduct.name}</strong><Link to={`/products/${featuredProduct.id}`}>Ver produto <span aria-hidden="true">↗</span></Link></div>}
        </div>
        <label className="hero-search">
          <Search size={20} strokeWidth={1.8} aria-hidden="true" />
          <span className="sr-only">Buscar produtos</span>
          <input type="search" aria-label="Buscar produtos" placeholder="Busque por produto, marca ou descrição"
            value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
          {searchInput && <button type="button" aria-label="Limpar busca" onClick={() => setSearchInput('')}><X size={17} aria-hidden="true" /></button>}
        </label>
        <div className="category-chips" id="catalog-categories" aria-label="Categorias em destaque">
          {categories.slice(0, 5).map((category) => <Link key={category.id} to={`/?categoryId=${category.id}`} className="category-chip">{category.name}</Link>)}
        </div>
      </section>

      <button className="filter-trigger" type="button" aria-expanded={filtersOpen} aria-controls="catalog-filters" onClick={() => setFiltersOpen(true)}>
        <Filter size={16} aria-hidden="true" /> Filtros
      </button>
      <div className="catalog-layout">
        {filtersOpen && <button className="filter-backdrop" type="button" aria-label="Fechar painel de filtros" onClick={() => setFiltersOpen(false)} />}
        <aside className={`filters${filtersOpen ? ' filters--open' : ''}`} id="catalog-filters" aria-label="Filtros do catálogo">
          <div className="filters__heading">
            <div><p className="eyebrow">EXPLORAR</p><h2>Refinar</h2></div>
            <div className="filters__actions"><button type="button" className="text-button" onClick={clearFilters}>Limpar</button><button type="button" className="filter-close" aria-label="Fechar filtros" onClick={() => setFiltersOpen(false)}><X size={18} aria-hidden="true" /></button></div>
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
          <div className="filter-drawer-actions"><button type="button" className="button button--primary" onClick={() => setFiltersOpen(false)}>Aplicar filtros</button></div>
        </aside>

        <section className="catalog-results" id="catalog-products" aria-label="Produtos">
          <div className="catalog-toolbar">
            <div><p className="eyebrow">CATÁLOGO</p><p className="catalog-count"><strong>{productCount.toLocaleString('pt-BR')}</strong> produtos encontrados</p></div>
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
          {hasInvalidPriceRange && <div className="notice notice--error" role="alert">O preço mínimo deve ser menor ou igual ao preço máximo.</div>}
          {error && <div className="notice notice--error" role="alert">{error} <button type="button" className="text-button" onClick={() => void loadProducts()}>Tentar novamente</button></div>}
          {categoriesError && <div className="notice notice--error" role="alert">As categorias não puderam ser carregadas. <button type="button" className="text-button" onClick={() => void loadCategories()}>Tentar novamente</button></div>}
          {isLoading ? (
            <div className="loading-grid" aria-label="Carregando produtos">
              {Array.from({ length: 6 }, (_, index) => <div className="skeleton" key={index} />)}
            </div>
          ) : products.length ? (
            <div className="product-grid">{products.map((product) => <ProductCard key={product.id} product={product} categoryName={categoryLabel(product.categoryId, categories)} />)}</div>
          ) : !error && !hasInvalidPriceRange ? (
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