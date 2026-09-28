import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useCatalogStore } from '../store/catalogStore';

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

function categoryLabel(categoryId: string, categories: ReturnType<typeof useCatalogStore.getState>['categories'], seen = new Set<string>()): string {
  const category = categories.find((item) => item.id === categoryId);
  if (!category || seen.has(category.id)) return '';
  seen.add(category.id);
  const parent = category.parentCategoryId ? categoryLabel(category.parentCategoryId, categories, seen) : '';
  return [parent, category.name].filter(Boolean).join(' / ');
}

export function ProductDetailPage() {
  const { id = '' } = useParams();
  const [imageFailed, setImageFailed] = useState(false);
  const product = useCatalogStore((state) => state.selectedProduct);
  const categories = useCatalogStore((state) => state.categories);
  const isLoading = useCatalogStore((state) => state.isLoading);
  const error = useCatalogStore((state) => state.error);

  useEffect(() => {
    setImageFailed(false);
    void useCatalogStore.getState().loadCategories();
    void useCatalogStore.getState().loadProduct(id);
  }, [id]);

  if (isLoading) return <main className="detail-state" aria-live="polite">Carregando produto…</main>;
  if (error || !product) {
    return <main className="detail-state"><p role="alert">{error ?? 'Produto não encontrado.'}</p><Link className="button button--outline" to="/">Voltar ao catálogo</Link></main>;
  }

  const category = categories.find((item) => item.id === product.categoryId);
  return (
    <main className="detail-shell">
      <nav className="breadcrumbs" aria-label="Navegação estrutural">
        <Link to="/">Catálogo</Link><span>/</span>
        {category && <><Link to={`/?categoryId=${category.id}`}>{categoryLabel(category.id, categories)}</Link><span>/</span></>}
        <span>{product.name}</span>
      </nav>
      <section className="product-detail">
        <div className="product-detail__media">
          {product.imageUrl && !imageFailed ? <img src={product.imageUrl} alt={product.name} onError={() => setImageFailed(true)} /> : <div className="product-detail__image-empty">TECHSTORE</div>}
        </div>
        <div className="product-detail__content">
          <p className="eyebrow">{product.brand || 'TECHSTORE'} / {product.sku}</p>
          <h1>{product.name}</h1>
          <p className="product-detail__description">{product.description || 'Produto selecionado do catálogo TechStore.'}</p>
          <p className="product-detail__price">{currency.format(product.price)}</p>
          <p className={product.quantity > 0 ? 'detail-stock' : 'detail-stock detail-stock--empty'}>
            <span aria-hidden="true" />{product.quantity > 0 ? `${product.quantity} disponíveis` : 'Indisponível'}
          </p>
          <button className="button button--primary" type="button" disabled aria-disabled="true">Adicionar ao carrinho</button>
          <p className="placeholder-note">Carrinho indisponível neste momento.</p>
          {category && <dl className="product-facts"><div><dt>Categoria</dt><dd>{categoryLabel(category.id, categories)}</dd></div><div><dt>Referência</dt><dd>{product.sku}</dd></div></dl>}
        </div>
      </section>
      <Link className="back-link" to="/">← Voltar ao catálogo</Link>
    </main>
  );
}