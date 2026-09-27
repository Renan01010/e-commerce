import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { ProductCard } from '../ProductCard';
import type { Product } from '../../types/catalog';

const product: Product = {
  id: 'p-1', name: 'Fone Studio', description: 'Som sem ruído', price: 399.9, cost: null,
  brand: 'Acme', sku: 'AC-1', categoryId: 'c-1', quantity: 3, imageUrl: null,
  isActive: true, createdAt: '', updatedAt: '',
};

describe('ProductCard', () => {
  it('shows product details and links to its page', () => {
    render(<MemoryRouter><ProductCard product={product} /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Fone Studio' })).toBeInTheDocument();
    expect(screen.getByText('R$ 399,90')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver Fone Studio' })).toHaveAttribute('href', '/products/p-1');
    expect(screen.getByText('Disponível')).toBeInTheDocument();
  });
});