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
};

const unavailableItem: CartItemData = {
  productId: 'product-2',
  quantity: 1,
  available: false,
  product: null,
};

describe('CartItem', () => {
  it('renders only the unavailable state and remove action when product summary is null', () => {
    const onQuantityChange = vi.fn();
    const onRemove = vi.fn();
    render(<CartItem item={unavailableItem} pending={false} onQuantityChange={onQuantityChange} onRemove={onRemove} />);

    expect(screen.getByText('Produto indisponível')).toBeInTheDocument();
    expect(screen.getByText('product-2')).toBeInTheDocument();
    expect(screen.getByText(/não está mais disponível/i)).toBeInTheDocument();
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
    fireEvent.click(screen.getByRole('button', { name: 'Aumentar quantidade de Keyboard' }));
    expect(onQuantityChange).toHaveBeenCalledWith('product-1', 3);

    expect(screen.getByRole('button', { name: 'Diminuir quantidade de Keyboard' })).toBeEnabled();
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
});
