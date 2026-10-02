export interface ProductSummary {
  name: string;
  price: number | null;
  brand: string | null;
  imageUrl: string | null;
}

export interface CartItem {
  productId: string;
  quantity: number;
  available: boolean;
  product: ProductSummary | null;
  unitPriceSnapshot: number | null;
  priceAvailable: boolean;
  subtotal: number | null;
}

export interface CartResponse {
  items: CartItem[];
  maxItemQuantity: number;
  total: number | null;
  totalAvailable: boolean;
}

export interface AddCartItemRequest {
  productId: string;
  quantity: number;
}

export interface SetCartItemQuantityRequest {
  quantity: number;
}
