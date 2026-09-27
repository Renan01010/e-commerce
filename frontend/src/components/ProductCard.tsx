import { Link } from 'react-router-dom';
import type { Product } from '../types/catalog';

interface ProductCardProps {
  product: Product;
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function ProductCard({ product }: ProductCardProps) {
  return (
    <article className="product-card">
      <Link className="product-card__link" to={`/products/${product.id}`} aria-label={`Ver ${product.name}`}>
        <div className="product-card__image-wrap">
          {product.imageUrl ? (
            <img className="product-card__image" src={product.imageUrl} alt={product.name} loading="lazy" />
          ) : (
            <div className="product-card__image-empty" aria-label="Imagem indisponível">TS</div>
          )}
          <span className={product.quantity > 0 ? 'stock-tag' : 'stock-tag stock-tag--empty'}>
            {product.quantity > 0 ? 'Disponível' : 'Esgotado'}
          </span>
        </div>
        <div className="product-card__body">
          <div className="product-card__meta">
            <span>{product.brand || 'TechStore'}</span>
            <span>{product.sku}</span>
          </div>
          <h2>{product.name}</h2>
          <p>{product.description || 'Veja os detalhes deste produto.'}</p>
          <strong>{currency.format(product.price)}</strong>
        </div>
      </Link>
    </article>
  );
}