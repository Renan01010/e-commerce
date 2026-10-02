import { create } from 'zustand';
import { cartApi } from '../services/apiClient';
import type { AddCartItemRequest, CartItem, CartResponse } from '../types/cart';
import { useAuthStore } from './authStore';

export type CartStatus = 'idle' | 'loading' | 'loaded' | 'error';

interface CartState {
  items: CartItem[];
  maxItemQuantity: number;
  total: number | null;
  totalAvailable: boolean;
  status: CartStatus;
  error: string | null;
  summaryRefreshError: string | null;
  successMessage: string | null;
  pendingOperations: Record<string, boolean>;
  loadCart: () => Promise<void>;
  addItem: (request: AddCartItemRequest) => Promise<boolean>;
  setQuantity: (productId: string, quantity: number) => Promise<boolean>;
  removeItem: (productId: string) => Promise<boolean>;
  clearCart: () => Promise<boolean>;
  retrySummary: () => Promise<void>;
  dismissFeedback: () => void;
}

interface PendingLoad {
  session: NonNullable<ReturnType<typeof useAuthStore.getState>['session']>;
  requestId: number;
  promise: Promise<void>;
}

let activeSession: NonNullable<ReturnType<typeof useAuthStore.getState>['session']> | null = null;
let pendingLoad: PendingLoad | null = null;
let requestVersion = 0;
let expirationTimer: ReturnType<typeof setTimeout> | undefined;

const positiveCartQuantity = (quantity: number, maximum: number) => Number.isInteger(quantity)
  && quantity > 0
  && quantity <= maximum;

function messageFrom(error: unknown): string {
  return error instanceof Error ? error.message : 'O carrinho não pôde ser atualizado. Tente novamente.';
}

function resetForSession(session: typeof activeSession) {
  if (activeSession === session) return;

  activeSession = session;
  requestVersion += 1;
  pendingLoad = null;
  if (expirationTimer !== undefined) clearTimeout(expirationTimer);

  useCartStore.setState({
    items: [],
    maxItemQuantity: 99,
    total: null,
    totalAvailable: false,
    status: 'idle',
    error: null,
    summaryRefreshError: null,
    successMessage: null,
    pendingOperations: {},
  });

  if (session) {
    const delay = Math.max(0, session.expiresAt - Date.now());
    expirationTimer = setTimeout(() => {
      if (useAuthStore.getState().session === session) useAuthStore.getState().clearSession();
    }, delay);
  }
}

function currentValidSession() {
  const session = useAuthStore.getState().getValidSession();
  resetForSession(session);
  return session;
}

function hasPendingOperations() {
  return Object.values(useCartStore.getState().pendingOperations).some(Boolean);
}

async function runMutation(
  operationKey: string,
  request: () => Promise<CartResponse | void>,
  successMessage: string,
  applyResponse: (response: CartResponse | void) => CartItem[],
): Promise<boolean> {
  const session = currentValidSession();
  if (!session) {
    useCartStore.setState({ error: 'Entre na sua conta para acessar o carrinho.' });
    return false;
  }
  if (hasPendingOperations()) return false;

  const generation = ++requestVersion;
  pendingLoad = null;
  const previousStatus = useCartStore.getState().status;
  useCartStore.setState((state) => ({
    error: null,
    summaryRefreshError: null,
    successMessage: null,
    pendingOperations: { ...state.pendingOperations, [operationKey]: true },
  }));

  let succeeded = false;
  try {
    const response = await request();
    if (generation === requestVersion && activeSession === session) {
        const items = applyResponse(response);
        const cartResponse = response && typeof response === 'object' && 'items' in response
          ? response as CartResponse
          : null;
        const financialState = cartResponse
          ? {
            maxItemQuantity: cartResponse.maxItemQuantity,
            total: cartResponse.total,
            totalAvailable: cartResponse.totalAvailable,
          }
          : {
            maxItemQuantity: useCartStore.getState().maxItemQuantity,
            total: null,
            totalAvailable: false,
          };
      useCartStore.setState({
          items,
          ...financialState,
        status: 'loaded',
        error: null,
        summaryRefreshError: null,
        successMessage,
      });
      succeeded = true;
    }
  } catch (error) {
    if (generation === requestVersion && activeSession === session) {
      const message = messageFrom(error);
      if (error instanceof Error && 'kind' in error && error.kind === 'unauthenticated') {
        useAuthStore.getState().clearSession();
      } else {
        useCartStore.setState({
          status: previousStatus === 'loaded' ? 'loaded' : 'error',
          error: message,
          successMessage: null,
        });
      }
    }
  } finally {
    if (generation === requestVersion && activeSession === session) {
      useCartStore.setState((state) => {
        const pendingOperations = { ...state.pendingOperations };
        delete pendingOperations[operationKey];
        return { pendingOperations };
      });
    }
  }
  return succeeded;
}

async function runDeleteAndRefresh(
  operationKey: string,
  request: () => Promise<void>,
  successMessage: string,
  selectItems: (items: CartItem[]) => CartItem[],
): Promise<boolean> {
  const session = currentValidSession();
  if (!session) {
    useCartStore.setState({ error: 'Entre na sua conta para acessar o carrinho.' });
    return false;
  }
  if (hasPendingOperations()) return false;

  const generation = ++requestVersion;
  pendingLoad = null;
  useCartStore.setState((state) => ({
    error: null,
    summaryRefreshError: null,
    successMessage: null,
    pendingOperations: { ...state.pendingOperations, [operationKey]: true },
  }));

  let deletionConfirmed = false;
  try {
    await request();
    if (generation !== requestVersion || activeSession !== session) return false;

    const state = useCartStore.getState();
    useCartStore.setState({
      items: selectItems(state.items),
      maxItemQuantity: state.maxItemQuantity,
      total: null,
      totalAvailable: false,
      status: 'loaded',
      error: null,
      summaryRefreshError: null,
      successMessage,
    });
    deletionConfirmed = true;

    try {
      const response = await cartApi.getCart();
      if (generation === requestVersion && activeSession === session) {
        useCartStore.setState({
          items: response.items,
          maxItemQuantity: response.maxItemQuantity,
          total: response.total,
          totalAvailable: response.totalAvailable,
          status: 'loaded',
          error: null,
          summaryRefreshError: null,
          successMessage,
        });
      }
    } catch (error) {
      if (generation === requestVersion && activeSession === session) {
        if (error instanceof Error && 'kind' in error && error.kind === 'unauthenticated') {
          useAuthStore.getState().clearSession();
        } else {
          useCartStore.setState({
            total: null,
            totalAvailable: false,
            summaryRefreshError: messageFrom(error),
          });
        }
      }
    }
  } catch (error) {
    if (generation === requestVersion && activeSession === session) {
      const message = messageFrom(error);
      if (error instanceof Error && 'kind' in error && error.kind === 'unauthenticated') {
        useAuthStore.getState().clearSession();
      } else {
        useCartStore.setState({ error: message, successMessage: null });
      }
    }
  } finally {
    if (generation === requestVersion && activeSession === session) {
      useCartStore.setState((state) => {
        const pendingOperations = { ...state.pendingOperations };
        delete pendingOperations[operationKey];
        return { pendingOperations };
      });
    }
  }
  return deletionConfirmed;
}

async function retryCartSummary(): Promise<void> {
  const session = currentValidSession();
  if (!session || hasPendingOperations()) return;

  const generation = ++requestVersion;
  useCartStore.setState((state) => ({
    pendingOperations: { ...state.pendingOperations, refresh: true },
  }));
  try {
    const response = await cartApi.getCart();
    if (generation === requestVersion && activeSession === session) {
      useCartStore.setState({
        items: response.items,
        maxItemQuantity: response.maxItemQuantity,
        total: response.total,
        totalAvailable: response.totalAvailable,
        status: 'loaded',
        error: null,
        summaryRefreshError: null,
      });
    }
  } catch (error) {
    if (generation === requestVersion && activeSession === session) {
      if (error instanceof Error && 'kind' in error && error.kind === 'unauthenticated') {
        useAuthStore.getState().clearSession();
      } else {
        useCartStore.setState({ summaryRefreshError: messageFrom(error) });
      }
    }
  } finally {
    if (generation === requestVersion && activeSession === session) {
      useCartStore.setState((state) => {
        const pendingOperations = { ...state.pendingOperations };
        delete pendingOperations.refresh;
        return { pendingOperations };
      });
    }
  }
}

export const useCartStore = create<CartState>((set, get) => ({
  items: [],
  maxItemQuantity: 99,
  total: null,
  totalAvailable: false,
  status: 'idle',
  error: null,
  summaryRefreshError: null,
  successMessage: null,
  pendingOperations: {},

  loadCart: async () => {
    const session = currentValidSession();
    if (!session) return;

    const state = get();
    if (state.status === 'loaded') return;
    if (pendingLoad?.session === session) return pendingLoad.promise;

    const requestId = ++requestVersion;
    set({ status: 'loading', error: null, successMessage: null });
    const promise = (async () => {
      try {
        const response = await cartApi.getCart();
        if (requestId === requestVersion && activeSession === session) {
          set({
            items: response.items,
            maxItemQuantity: response.maxItemQuantity,
            total: response.total,
            totalAvailable: response.totalAvailable,
            status: 'loaded',
            error: null,
            summaryRefreshError: null,
          });
        }
      } catch (error) {
        if (requestId === requestVersion && activeSession === session) {
          if (error instanceof Error && 'kind' in error && error.kind === 'unauthenticated') {
            useAuthStore.getState().clearSession();
          } else {
            set({ status: 'error', error: messageFrom(error) });
          }
        }
      } finally {
        if (pendingLoad?.requestId === requestId) pendingLoad = null;
      }
    })();
    pendingLoad = { session, requestId, promise };
    return promise;
  },

  addItem: (request) => {
    const { status, maxItemQuantity, items } = get();
    if (status !== 'loaded') {
      set({ error: 'Aguarde o carregamento do carrinho antes de adicionar produtos.' });
      return Promise.resolve(false);
    }
    const currentQuantity = items.find((item) => item.productId === request.productId)?.quantity ?? 0;
    if (!positiveCartQuantity(request.quantity, maxItemQuantity)
      || currentQuantity + request.quantity > maxItemQuantity) {
      set({ error: Number.isInteger(request.quantity) && request.quantity > 0
        ? `A quantidade máxima por produto é ${maxItemQuantity}.`
        : 'Informe uma quantidade inteira positiva.' });
      return Promise.resolve(false);
    }
    return runMutation(
      `add:${request.productId}`,
      () => cartApi.addItem(request),
      'Produto adicionado ao carrinho.',
      (response) => (response as CartResponse).items,
    );
  },

  setQuantity: (productId, quantity) => {
    const { maxItemQuantity } = get();
    if (!positiveCartQuantity(quantity, maxItemQuantity)) {
      set({ error: Number.isInteger(quantity) && quantity > 0
        ? `A quantidade máxima por produto é ${maxItemQuantity}.`
        : 'Informe uma quantidade inteira positiva.' });
      return Promise.resolve(false);
    }
    return runMutation(
      `quantity:${productId}`,
      () => cartApi.setQuantity(productId, quantity),
      'Quantidade atualizada.',
      (response) => (response as CartResponse).items,
    );
  },

  removeItem: (productId) => runDeleteAndRefresh(
    `remove:${productId}`,
    () => cartApi.removeItem(productId),
    'Produto removido do carrinho.',
    (items) => items.filter((item) => item.productId !== productId),
  ),

  clearCart: () => runDeleteAndRefresh(
    'clear',
    () => cartApi.clearCart(),
    'Carrinho limpo.',
    () => [],
  ),

  retrySummary: retryCartSummary,

  dismissFeedback: () => set({ error: null, successMessage: null }),
}));

useAuthStore.subscribe((state, previousState) => {
  if (state.session !== previousState.session) {
    const session = state.session && state.session.expiresAt > Date.now() ? state.session : null;
    resetForSession(session);
  }
});
