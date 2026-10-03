import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CartItem } from '../CartItem';
import type { CartItem as CartItemData } from '../../../types/cart';

const availableItem: CartItemData = {
  productId: 'product-1',
  quantity: 2,
  available: true,
  product: { name: 'Keyboard', price: 49.9, brand: 'Acme', imageUrl: null },
  unitPriceSnapshot: 49.9,
  priceAvailable: true,
  subtotal: 99.8,
};

const unavailableItem: CartItemData = {
  productId: 'product-2',
  quantity: 1,
  available: false,
  product: null,
  unitPriceSnapshot: null,
  priceAvailable: false,
  subtotal: null,
};

const unknownPriceItem: CartItemData = {
  productId: 'product-3',
  quantity: 2,
  available: true,
  product: { name: 'Mouse', price: null, brand: 'Acme', imageUrl: null },
  unitPriceSnapshot: null,
  priceAvailable: false,
  subtotal: null,
};

const inactiveKnownPriceItem: CartItemData = {
  productId: 'product-4',
  quantity: 2,
  available: false,
  product: null,
  unitPriceSnapshot: 12.34,
  priceAvailable: true,
  subtotal: 24.68,
};

describe('CartItem', () => {
  it('renders only the unavailable state and remove action when product summary is null', () => {
    const onQuantityChange = vi.fn();
    const onRemove = vi.fn();
    render(<CartItem item={unavailableItem} pending={false} onQuantityChange={onQuantityChange} onRemove={onRemove} />);

    expect(screen.getByText('Produto indisponível')).toBeInTheDocument();
    expect(screen.queryByText('product-2')).not.toBeInTheDocument();
    expect(screen.getByText(/não está mais disponível/i)).toBeInTheDocument();
    expect(screen.getByText(/preço e subtotal são desconhecidos/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /quantidade/i })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /remover/i }));
    expect(onRemove).toHaveBeenCalledWith('product-2');
    expect(onQuantityChange).not.toHaveBeenCalled();
  });

  it('renders current summary data and emits positive quantity changes', () => {
    const onQuantityChange = vi.fn();
    render(<CartItem item={availableItem} pending={false} onQuantityChange={onQuantityChange} onRemove={vi.fn()} />);

    expect(screen.getByText('Keyboard')).toBeInTheDocument();
    expect(screen.getByText('Acme')).toBeInTheDocument();
    expect(screen.getByText(/49,90/)).toBeInTheDocument();
    expect(screen.getByText('/ unidade')).toBeInTheDocument();
    expect(screen.getByText(/subtotal.*99,80/i)).toBeInTheDocument();
    expect(screen.queryByText('product-1')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Aumentar quantidade de Keyboard' }));
    expect(onQuantityChange).toHaveBeenCalledWith('product-1', 3);

    expect(screen.getByRole('button', { name: 'Diminuir quantidade de Keyboard' })).toBeEnabled();
  });

  it('explains unknown price while preserving quantity and removal actions', () => {
    const onQuantityChange = vi.fn();
    const onRemove = vi.fn();
    render(<CartItem item={unknownPriceItem} pending={false} maxItemQuantity={4}
      onQuantityChange={onQuantityChange} onRemove={onRemove} />);

    expect(screen.getByText(/Preço e subtotal indisponíveis/)).toBeInTheDocument();
    expect(screen.queryByText(/R\$\s*0,00/)).not.toBeInTheDocument();
    expect(screen.queryByText('product-3')).not.toBeInTheDocument();
    expect(screen.getByRole('spinbutton', { name: 'Quantidade de Mouse' })).toHaveValue(2);
    expect(screen.getByRole('button', { name: 'Aumentar quantidade de Mouse' })).toBeEnabled();
    fireEvent.click(screen.getByRole('button', { name: 'Remover Mouse' }));
    expect(onRemove).toHaveBeenCalledWith('product-3');
  });

  it('shows a known snapshot subtotal even if the catalog product is inactive', () => {
    render(<CartItem item={inactiveKnownPriceItem} pending={false} maxItemQuantity={99}
      onQuantityChange={vi.fn()} onRemove={vi.fn()} />);

    expect(screen.getByText(/12,34/)).toBeInTheDocument();
    expect(screen.getByText(/24,68/)).toBeInTheDocument();
    expect(screen.getByText(/não está mais disponível/i)).toBeInTheDocument();
    expect(screen.queryByText('product-4')).not.toBeInTheDocument();
    expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument();
  });

  it('disables increment at the effective maxItemQuantity', () => {
    const atLimit = { ...availableItem, quantity: 4 };
    render(<CartItem item={atLimit} pending={false} maxItemQuantity={4}
      onQuantityChange={vi.fn()} onRemove={vi.fn()} />);

    expect(screen.getByRole('spinbutton', { name: 'Quantidade de Keyboard' })).toHaveAttribute('max', '4');
    expect(screen.getByRole('button', { name: 'Aumentar quantidade de Keyboard' })).toBeDisabled();
  });

  it('prevents decrementing below one and disables pending item actions', () => {
    const onQuantityChange = vi.fn();
    const minimumItem = { ...availableItem, quantity: 1 };
    const { rerender } = render(<CartItem item={minimumItem} pending={false} onQuantityChange={onQuantityChange} onRemove={vi.fn()} />);
    const decrement = screen.getByRole('button', { name: 'Diminuir quantidade de Keyboard' });
    expect(decrement).toBeDisabled();

    rerender(<CartItem item={minimumItem} pending onQuantityChange={onQuantityChange} onRemove={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Aumentar quantidade de Keyboard' })).toBeDisabled();
    expect(screen.getByRole('button', { name: /remover/i })).toBeDisabled();
    expect(onQuantityChange).not.toHaveBeenCalled();
  });

  it('uses product text as image alternative and supports keyboard removal', async () => {
    const user = userEvent.setup();
    const onRemove = vi.fn();
    const itemWithImage = {
      ...availableItem,
      product: { ...availableItem.product!, imageUrl: 'https://example.test/keyboard.png' },
    };
    render(<CartItem item={itemWithImage} pending={false} onQuantityChange={vi.fn()} onRemove={onRemove} />);

    expect(screen.getByRole('img', { name: 'Keyboard' })).toHaveAttribute('src', 'https://example.test/keyboard.png');
    const removeButton = screen.getByRole('button', { name: 'Remover Keyboard' });
    removeButton.focus();
    await user.keyboard('{Enter}');
    expect(onRemove).toHaveBeenCalledWith('product-1');
  });

  it('exposes an accessible fallback when the product image is missing or fails', () => {
    const { rerender } = render(<CartItem item={availableItem} pending={false}
      onQuantityChange={vi.fn()} onRemove={vi.fn()} />);
    expect(screen.getByRole('img', { name: 'Imagem indisponível para Keyboard' })).toBeInTheDocument();

    const itemWithImage = {
      ...availableItem,
      product: { ...availableItem.product!, imageUrl: 'https://example.test/keyboard.png' },
    };
    rerender(<CartItem item={itemWithImage} pending={false}
      onQuantityChange={vi.fn()} onRemove={vi.fn()} />);
    fireEvent.error(screen.getByRole('img', { name: 'Keyboard' }));

    expect(screen.getByRole('img', { name: 'Imagem indisponível para Keyboard' })).toBeInTheDocument();
  });
});
