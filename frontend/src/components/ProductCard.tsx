import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { Product } from '../types/catalog';

interface ProductCardProps {
  product: Product;
  categoryName?: string;
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function ProductCard({ product, categoryName }: ProductCardProps) {
  const [imageFailed, setImageFailed] = useState(false);

  return (
    <article className="product-card">
      <Link className="product-card__link" to={`/products/${product.id}`} aria-label={`Ver ${product.name}`}>
        <div className="product-card__image-wrap">
          {product.imageUrl && !imageFailed ? (
            <img className="product-card__image" src={product.imageUrl} alt={product.name} loading="lazy" onError={() => setImageFailed(true)} />
          ) : (
            <div className="product-card__image-empty" aria-label="Imagem indisponível">TS</div>
          )}
          <span className={product.quantity > 0 ? 'stock-tag' : 'stock-tag stock-tag--empty'}>
            {product.quantity > 0 ? 'Disponível' : 'Esgotado'}
          </span>
        </div>
        <div className="product-card__body">
          <div className="product-card__meta">
            <span>{categoryName || product.brand || 'TechStore'}</span>
            <span>{product.sku}</span>
          </div>
          <h2>{product.name}</h2>
          <p className="product-card__description">{product.description || 'Veja os detalhes deste produto.'}</p>
          <div className="product-card__price-row"><strong>{currency.format(product.price)}</strong><span className="product-card__arrow" aria-hidden="true">↗</span></div>
        </div>
      </Link>
    </article>
  );
}