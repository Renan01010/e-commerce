import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react';
import { LogOut, Menu, Search, ShoppingBag, UserRound, X } from 'lucide-react';
import { Link, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { CatalogPage } from './pages/CatalogPage';
import { HomePage } from './pages/HomePage';
import { CartPage } from './pages/CartPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { RegisterPage } from './pages/RegisterPage';
import { VerifyEmailPage } from './pages/VerifyEmailPage';
import { ResendVerificationPage } from './pages/ResendVerificationPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { AccountPage } from './pages/AccountPage';
import { AccountSecurityPage } from './pages/AccountSecurityPage';
import { RequireAuth } from './components/auth/RequireAuth';
import { accountService } from './services/accountService';
import { useAuthStore } from './store/authStore';
import { useCartStore } from './store/cartStore';
import { useCatalogStore } from './store/catalogStore';

useAuthStore.subscribe((state, previousState) => {
  if (state.session !== previousState.session) {
    useCartStore.getState().resetLocalCart();
    useCatalogStore.getState().resetCatalog();
    void useCatalogStore.getState().loadCategories();
  }
});

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/verify-email/resend" element={<ResendVerificationPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/account" element={<RequireAuth><StoreLayout><AccountPage /></StoreLayout></RequireAuth>} />
      <Route path="/account/security" element={<RequireAuth><StoreLayout><AccountSecurityPage /></StoreLayout></RequireAuth>} />
      <Route path="/" element={<StoreLayout><HomePage /></StoreLayout>} />
      <Route path="/catalog" element={<StoreLayout><CatalogPage /></StoreLayout>} />
      <Route path="/products/:id" element={<StoreLayout><ProductDetailPage /></StoreLayout>} />
      <Route path="/cart" element={<StoreLayout><CartPage /></StoreLayout>} />
      <Route path="*" element={<StoreLayout><main className="detail-state"><h1>Página não encontrada</h1><Link className="button button--outline" to="/catalog">Ir ao catálogo</Link></main></StoreLayout>} />
    </Routes>
  );
}

function StoreLayout({ children }: { children: ReactNode }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [headerSearch, setHeaderSearch] = useState('');
  const [isProfileLoading, setIsProfileLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const session = useAuthStore((state) => state.session);
  const profile = useAuthStore((state) => state.profile);
  const clearSession = useAuthStore((state) => state.clearSession);
  const items = useCartStore((state) => state.items);
  const cartStatus = useCartStore((state) => state.status);
  const loadCart = useCartStore((state) => state.loadCart);
  const categories = useCatalogStore((state) => state.categories);
  const loadCategories = useCatalogStore((state) => state.loadCategories);
  const orderedCategories = useMemo(
    () => [...categories].sort((left, right) => left.displayOrder - right.displayOrder),
    [categories],
  );
  const cartUnits = items.reduce((sum, item) => sum + item.quantity, 0);
  const isAuthenticated = Boolean(session && session.expiresAt > Date.now());
  const hasConfirmedCart = Boolean(isAuthenticated && cartStatus === 'loaded');

  useEffect(() => {
    if (session && session.expiresAt > Date.now()) void loadCart();
  }, [session, loadCart]);

  useEffect(() => {
    if (!isAuthenticated || profile) {
      setIsProfileLoading(false);
      return;
    }

    let active = true;
    setIsProfileLoading(true);
    void accountService.getProfile()
      .then((loadedProfile) => {
        if (active && useAuthStore.getState().getValidSession() === session) {
          useAuthStore.getState().setProfile(loadedProfile);
        }
      })
      .catch(() => {
        if (active && useAuthStore.getState().getValidSession() === session) {
          console.warn('Unable to load the authenticated profile for the header.');
        }
      })
      .finally(() => {
        if (active) setIsProfileLoading(false);
      });

    return () => {
      active = false;
    };
  }, [isAuthenticated, profile, session]);

  useEffect(() => {
    void loadCategories();
  }, [loadCategories]);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    setHeaderSearch(location.pathname === '/catalog' ? params.get('query') ?? '' : '');
  }, [location.pathname, location.search]);

  function submitHeaderSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const query = headerSearch.trim();
    navigate(query ? `/catalog?query=${encodeURIComponent(query)}` : '/catalog');
    setMenuOpen(false);
  }

  function logout() {
    clearSession();
    navigate('/', { replace: true });
  }

  return (
    <div className={`app-frame${location.pathname === '/' ? ' app-frame--home' : ''}`}>
      <header className="site-header">
        <div className="site-header__main">
          <Link className="brand-mark" to="/" aria-label="TechStore, página inicial">
            <span className="brand-mark__symbol">T</span><span>techstore</span>
          </Link>
          {location.pathname !== '/' && (
            <form className="header-search" role="search" onSubmit={submitHeaderSearch}>
              <label className="sr-only" htmlFor="header-product-search">Buscar produtos</label>
              <input id="header-product-search" type="search" placeholder="Buscar produtos, marcas ou categorias..." value={headerSearch} onChange={(event) => setHeaderSearch(event.target.value)} />
              <button type="submit" aria-label="Buscar no catálogo"><Search size={19} aria-hidden="true" /></button>
            </form>
          )}
          <nav className={`site-nav${menuOpen ? ' site-nav--open' : ''}`} id="main-navigation" aria-label="Navegação principal">
            <Link className="site-nav__link" to="/" onClick={() => setMenuOpen(false)}>Início</Link>
            <Link className="site-nav__link" to="/catalog" onClick={() => setMenuOpen(false)}>Produtos</Link>
            <a className="site-nav__link" href={location.pathname === '/' ? '#home-categories' : location.pathname === '/catalog' ? '#catalog-categories' : '/catalog#catalog-categories'} onClick={() => setMenuOpen(false)}>Categorias</a>
          </nav>
        </div>
        <div className="site-actions">
          {isAuthenticated ? (
            <>
              <Link className="header-action" to="/account" aria-label="Minha conta">
                <span className="account-avatar" aria-hidden="true">{profile?.name?.trim().charAt(0).toUpperCase() || <UserRound size={16} />}</span>
                <span aria-busy={isProfileLoading}>
                  {profile?.name?.trim() || (isProfileLoading ? 'Carregando...' : 'Minha conta')}
                </span>
              </Link>
              <button className="header-action" type="button" onClick={logout}>
                <LogOut size={17} strokeWidth={1.8} aria-hidden="true" /><span>Sair</span>
              </button>
            </>
          ) : (
            <>
              <Link className="header-action" to="/login" aria-label="Entrar na minha conta">
                <UserRound size={17} strokeWidth={1.8} aria-hidden="true" /><span>Entrar</span>
              </Link>
              <Link className="header-action header-action--muted" to="/register">Criar conta</Link>
            </>
          )}
          <Link className="header-action header-cart" to="/cart" aria-label={hasConfirmedCart ? `Carrinho, ${cartUnits} ${cartUnits === 1 ? 'unidade' : 'unidades'}` : 'Carrinho'}>
            <ShoppingBag size={17} strokeWidth={1.8} aria-hidden="true" /><span>Carrinho</span>
            {hasConfirmedCart && <small aria-hidden="true">{cartUnits}</small>}
          </Link>
          <button className="mobile-menu-toggle" type="button" aria-expanded={menuOpen} aria-controls="main-navigation" aria-label={menuOpen ? 'Fechar menu' : 'Abrir menu'} onClick={() => setMenuOpen((open) => !open)}>
            {menuOpen ? <X size={19} aria-hidden="true" /> : <Menu size={19} aria-hidden="true" />}
          </button>
        </div>
        <nav className="category-nav" aria-label="Categorias do catálogo">
          <Link to="/catalog" className="category-nav__all">Todas as categorias</Link>
          {orderedCategories.slice(0, 8).map((category) => (
            <Link key={category.id} to={`/catalog?categoryId=${encodeURIComponent(category.id)}`}>{category.name}</Link>
          ))}
        </nav>
      </header>
      {children}
    </div>
  );
}