import { useState } from 'react';
import { AlertCircle, ImageOff, Minus, Plus, Trash2 } from 'lucide-react';
import type { CartItem as CartItemData } from '../../types/cart';

interface CartItemProps {
  item: CartItemData;
  pending: boolean;
  onQuantityChange: (productId: string, quantity: number) => void;
  onRemove: (productId: string) => void;
  maxItemQuantity?: number;
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function CartItem({ item, pending, maxItemQuantity = 99, onQuantityChange, onRemove }: CartItemProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const product = item.product;
  const unavailable = !item.available || !product;
  const priceKnown = item.priceAvailable && item.unitPriceSnapshot !== null;
  const subtotalKnown = item.priceAvailable && item.subtotal !== null;
  const label = product?.name ?? 'Produto indisponível';

  return (
    <article className={`cart-item${unavailable ? ' cart-item--unavailable' : ''}`}>
      <div className="cart-item__media">
        {!unavailable && product.imageUrl && !imageFailed ? (
          <img src={product.imageUrl} alt={product.name} onError={() => setImageFailed(true)} />
        ) : (
          <div className="cart-item__image-empty" role="img" aria-label={unavailable ? 'Produto indisponível' : `Imagem indisponível para ${label}`}>
            {unavailable ? <AlertCircle size={22} aria-hidden="true" /> : <ImageOff size={22} aria-hidden="true" />}
          </div>
        )}
      </div>

      <div className="cart-item__details">
        <span className={`cart-item__availability${unavailable ? ' cart-item__availability--unavailable' : ''}`}>
          {unavailable ? 'Indisponível' : 'Disponível'}
        </span>
        <h2>{label}</h2>
        {unavailable ? (
          <div className="cart-item__warning" role="status">
            <AlertCircle size={16} aria-hidden="true" />
            <span>{priceKnown
              ? 'Este produto não está mais disponível. Você pode removê-lo do carrinho.'
              : 'Este produto não está mais disponível e seu preço e subtotal são desconhecidos. Você pode removê-lo do carrinho.'}</span>
          </div>
        ) : (
          product.brand && <p className="cart-item__brand">{product.brand}</p>
        )}
      </div>

      <div className="cart-item__actions">
        {priceKnown && (
          <strong className="cart-item__price">
            {currency.format(item.unitPriceSnapshot!)} <small>/ unidade</small>
          </strong>
        )}
        {subtotalKnown && <span className="cart-item__subtotal">Subtotal {currency.format(item.subtotal!)}</span>}
        {priceKnown && !subtotalKnown && (
          <span className="cart-item__subtotal">Subtotal indisponível</span>
        )}
        {!priceKnown && (
          <div className="cart-item__warning" role="status">
            <AlertCircle size={16} aria-hidden="true" />
            <span>Preço e subtotal indisponíveis. Você pode remover o produto.</span>
          </div>
        )}
        {!unavailable && (
          <div className="cart-quantity" role="group" aria-label={`Controles de quantidade de ${product.name}`}>
            <button
              type="button"
              aria-label={`Diminuir quantidade de ${product.name}`}
              disabled={pending || item.quantity <= 1}
              onClick={() => onQuantityChange(item.productId, item.quantity - 1)}
            >
              <Minus size={15} aria-hidden="true" />
            </button>
            <input aria-label={`Quantidade de ${product.name}`} type="number" min="1" max={maxItemQuantity} value={item.quantity} readOnly />
            <button
              type="button"
              aria-label={`Aumentar quantidade de ${product.name}`}
              disabled={pending || item.quantity >= maxItemQuantity}
              onClick={() => onQuantityChange(item.productId, item.quantity + 1)}
            >
              <Plus size={15} aria-hidden="true" />
            </button>
          </div>
        )}
        <button
          className="cart-item__remove"
          type="button"
          aria-label={`Remover ${label}`}
          disabled={pending}
          onClick={() => onRemove(item.productId)}
        >
          <Trash2 size={16} aria-hidden="true" />
          <span>{pending ? 'Aguarde…' : 'Remover'}</span>
        </button>
      </div>
    </article>
  );
}
