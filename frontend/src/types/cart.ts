export interface ProductSummary {
  name: string;
  price: number;
  brand: string | null;
  imageUrl: string | null;
}

export interface CartItem {
  productId: string;
  quantity: number;
  available: boolean;
  product: ProductSummary | null;
}

export interface CartResponse {
  items: CartItem[];
}

export interface AddCartItemRequest {
  productId: string;
  quantity: number;
}

export interface SetCartItemQuantityRequest {
  quantity: number;
}
