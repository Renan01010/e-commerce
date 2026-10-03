import { useState } from 'react';
import { LoaderCircle, ShoppingCart } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { Product } from '../types/catalog';
import { useAuthStore } from '../store/authStore';
import { useCartStore } from '../store/cartStore';

interface ProductCardProps {
  product: Product;
  categoryName?: string;
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function ProductCard({ product, categoryName }: ProductCardProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const [feedback, setFeedback] = useState<{ message: string; error: boolean } | null>(null);
  const session = useAuthStore((state) => state.session);
  const addItem = useCartStore((state) => state.addItem);
  const loadCart = useCartStore((state) => state.loadCart);
  const cartStatus = useCartStore((state) => state.status);
  const cartError = useCartStore((state) => state.error);
  const pending = useCartStore((state) => Boolean(state.pendingOperations[`add:${product.id}`]));
  const authenticated = Boolean(session && session.expiresAt > Date.now());
  const canAdd = authenticated && cartStatus === 'loaded' && product.quantity > 0 && !pending;

  async function handleAdd() {
    setFeedback(null);
    const added = await addItem({ productId: product.id, quantity: 1 });
    const state = useCartStore.getState();
    const message = added ? state.successMessage : state.error;
    if (message) setFeedback({ message, error: !added });
  }

  return (
    <article className="product-card">
      <Link className="product-card__link" to={`/products/${product.id}`} aria-label={`Ver ${product.name}`}>
        <div className="product-card__image-wrap">
          {product.imageUrl && !imageFailed ? (
            <img className="product-card__image" src={product.imageUrl} alt={product.name} loading="lazy" onError={() => setImageFailed(true)} />
          ) : (
            <div className="product-card__image-empty" role="img" aria-label={`Imagem indisponível para ${product.name}`}>TS</div>
          )}
          <span className={product.quantity > 0 ? 'stock-tag' : 'stock-tag stock-tag--empty'}>
            {product.quantity > 0 ? 'Disponível' : 'Esgotado'}
          </span>
        </div>
        <div className="product-card__body">
          <div className="product-card__meta">
            {categoryName && <span>{categoryName}</span>}
            {product.brand && <span>{product.brand}</span>}
            {!categoryName && !product.brand && <span>Produto</span>}
          </div>
          <h2>{product.name}</h2>
          <p className="product-card__description">{product.description || 'Veja os detalhes deste produto.'}</p>
          <div className="product-card__price-row"><strong>{currency.format(product.price)}</strong><span className="product-card__arrow" aria-hidden="true">↗</span></div>
        </div>
      </Link>
      <div className="product-card__actions">
        {authenticated ? (
          <button className="product-card__add" type="button" disabled={!canAdd}
            onClick={() => void handleAdd()} aria-label={`Adicionar ${product.name} ao carrinho`}>
            {pending ? <LoaderCircle size={15} className="spin" aria-hidden="true" /> : <ShoppingCart size={15} aria-hidden="true" />}
            {pending ? 'Adicionando…' : product.quantity <= 0 ? 'Indisponível' : cartStatus === 'loaded' ? 'Adicionar ao carrinho' : 'Aguarde o carrinho'}
          </button>
        ) : (
          <Link className="product-card__add" to="/login" aria-label={`Entre na sua conta para adicionar ${product.name} ao carrinho`}>
            <ShoppingCart size={15} aria-hidden="true" /> Entrar para comprar
          </Link>
        )}
        {authenticated && cartStatus === 'error' && (
          <button type="button" className="product-card__retry" onClick={() => void loadCart()}>Tentar carregar carrinho</button>
        )}
        {feedback && <p className={`product-card__feedback${feedback.error ? ' product-card__feedback--error' : ''}`} role={feedback.error ? 'alert' : 'status'}>{feedback.message}</p>}
        {authenticated && cartStatus === 'loading' && <span className="sr-only" role="status">Carregando carrinho</span>}
        {authenticated && cartStatus === 'error' && cartError && <span className="sr-only" role="alert">{cartError}</span>}
      </div>
    </article>
  );
}