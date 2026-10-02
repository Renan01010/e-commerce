import { useEffect, useState } from 'react';
import { AlertCircle, ArrowRight, RefreshCw, ShoppingBag, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { CartItem } from '../components/cart/CartItem';
import { ClearCartConfirmation } from '../components/cart/ClearCartConfirmation';
import { useAuthStore } from '../store/authStore';
import { useCartStore } from '../store/cartStore';

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function CartPage() {
  const [confirmClear, setConfirmClear] = useState(false);
  const session = useAuthStore((state) => state.session);
  const items = useCartStore((state) => state.items);
  const maxItemQuantity = useCartStore((state) => state.maxItemQuantity);
  const total = useCartStore((state) => state.total);
  const totalAvailable = useCartStore((state) => state.totalAvailable);
  const status = useCartStore((state) => state.status);
  const error = useCartStore((state) => state.error);
  const successMessage = useCartStore((state) => state.successMessage);
  const pendingOperations = useCartStore((state) => state.pendingOperations);
  const loadCart = useCartStore((state) => state.loadCart);
  const setQuantity = useCartStore((state) => state.setQuantity);
  const removeItem = useCartStore((state) => state.removeItem);
  const clearCart = useCartStore((state) => state.clearCart);
  const dismissFeedback = useCartStore((state) => state.dismissFeedback);
  const isAuthenticated = Boolean(session && session.expiresAt > Date.now());
  const units = items.reduce((sum, item) => sum + item.quantity, 0);
  const mutationPending = Object.values(pendingOperations).some(Boolean);

  useEffect(() => {
    if (isAuthenticated) void loadCart();
  }, [isAuthenticated, loadCart]);

  async function confirmClearCart() {
    const cleared = await clearCart();
    if (cleared) setConfirmClear(false);
  }

  if (!isAuthenticated) {
    return (
      <main className="cart-shell">
        <CartBreadcrumb />
        <section className="cart-guest empty-state">
          <ShoppingBag size={34} aria-hidden="true" />
          <h1>Seu carrinho</h1>
          <p>Entre na sua conta para ver seu carrinho.</p>
          <Link className="button button--primary" to="/login">Fazer login <ArrowRight size={15} aria-hidden="true" /></Link>
        </section>
      </main>
    );
  }

  return (
    <main className="cart-shell">
      <CartBreadcrumb />
      <div className="cart-layout">
        <section className="cart-main" aria-labelledby="cart-title">
          <header className="cart-heading">
            <div>
              <h1 id="cart-title">Seu carrinho</h1>
              <p>Revise seus produtos e ajuste as quantidades antes de continuar.</p>
            </div>
          </header>

          <div className="cart-list-heading">
            <p>{items.length} {items.length === 1 ? 'produto no carrinho' : 'produtos no carrinho'}</p>
            {items.length > 0 && (
              <button className="cart-clear-link" type="button" disabled={mutationPending} onClick={() => { dismissFeedback(); setConfirmClear(true); }}>
                <Trash2 size={16} aria-hidden="true" /> Limpar carrinho
              </button>
            )}
          </div>

          {error && (status !== 'error' || items.length > 0) && (
            <div className="cart-notice cart-notice--error" role="alert">
              <AlertCircle size={18} aria-hidden="true" />
              <span>{error}</span>
              {status === 'error' && <button type="button" onClick={() => void loadCart()}>Tentar novamente</button>}
            </div>
          )}
          {successMessage && <p className="cart-notice cart-notice--success" role="status">{successMessage}</p>}

          {status === 'loading' || status === 'idle' ? (
            <div className="cart-loading" role="status" aria-label="Carregando carrinho">
              <span /><span /><span />
            </div>
          ) : status === 'error' && items.length === 0 ? (
            <div className="cart-error-state">
              <AlertCircle size={25} aria-hidden="true" />
              <h2>Não foi possível carregar o carrinho.</h2>
              <p>{error ?? 'Tente novamente em instantes.'}</p>
              <button className="button button--outline" type="button" onClick={() => void loadCart()}>
                <RefreshCw size={15} aria-hidden="true" /> Tentar novamente
              </button>
            </div>
          ) : items.length === 0 ? (
            <div className="cart-empty empty-state">
              <ShoppingBag size={34} aria-hidden="true" />
              <h2>Seu carrinho está vazio.</h2>
              <p>Encontre produtos para adicionar ao seu carrinho.</p>
              <Link className="button button--primary" to="/">Explorar produtos <ArrowRight size={15} aria-hidden="true" /></Link>
            </div>
          ) : (
            <div className="cart-items" aria-label="Produtos no carrinho">
              {items.map((item) => (
                <CartItem
                  key={item.productId}
                  item={item}
                  pending={mutationPending}
                  maxItemQuantity={maxItemQuantity}
                  onQuantityChange={(productId, quantity) => { dismissFeedback(); void setQuantity(productId, quantity); }}
                  onRemove={(productId) => { dismissFeedback(); void removeItem(productId); }}
                />
              ))}
            </div>
          )}
        </section>

        <aside className="cart-sidebar" aria-label="Resumo do carrinho">
          <section className="cart-summary-panel">
            <h2>Resumo do carrinho</h2>
            <div className="cart-summary-count">
              <ShoppingBag size={24} aria-hidden="true" />
              <p><strong>{items.length} {items.length === 1 ? 'produto' : 'produtos'}</strong><span>{units} {units === 1 ? 'unidade' : 'unidades'}</span></p>
            </div>
            <div className="cart-summary-total" aria-live="polite">
              <span>Total do carrinho</span>
              {totalAvailable && total !== null ? (
                <strong>{currency.format(total)}</strong>
              ) : (
                <p role="status">Total indisponível enquanto houver produto sem preço conhecido.</p>
              )}
            </div>
            <Link className="cart-continue button button--primary" to="/">
              <ShoppingBag size={16} aria-hidden="true" /> Continuar comprando
            </Link>
            {items.length > 0 && (
              <button className="cart-clear-secondary button button--outline" type="button" disabled={mutationPending} onClick={() => { dismissFeedback(); setConfirmClear(true); }}>
                <Trash2 size={16} aria-hidden="true" /> Limpar carrinho
              </button>
            )}
          </section>
        </aside>
      </div>

      {confirmClear && <ClearCartConfirmation pending={mutationPending} onCancel={() => setConfirmClear(false)} onConfirm={() => void confirmClearCart()} />}
    </main>
  );
}

function CartBreadcrumb() {
  return (
    <nav className="breadcrumbs cart-breadcrumb" aria-label="Navegação estrutural">
      <Link to="/">TechStore</Link><span aria-hidden="true">/</span><strong>Carrinho</strong>
    </nav>
  );
}
