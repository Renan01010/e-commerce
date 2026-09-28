import { useState, type ReactNode } from 'react';
import { Menu, ShoppingBag, UserRound, X } from 'lucide-react';
import { Link, Route, Routes } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { CatalogPage } from './pages/CatalogPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { RegisterUnavailablePage } from './pages/RegisterUnavailablePage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterUnavailablePage />} />
      <Route path="/" element={<StoreLayout><CatalogPage /></StoreLayout>} />
      <Route path="/products/:id" element={<StoreLayout><ProductDetailPage /></StoreLayout>} />
      <Route path="*" element={<StoreLayout><main className="detail-state"><h1>Página não encontrada</h1><Link className="button button--outline" to="/">Ir ao catálogo</Link></main></StoreLayout>} />
    </Routes>
  );
}

function StoreLayout({ children }: { children: ReactNode }) {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <div className="app-frame">
      <header className="site-header">
        <Link className="brand-mark" to="/" aria-label="TechStore, página inicial">
          <span className="brand-mark__symbol">T</span><span>techstore</span>
        </Link>
        <nav className={`site-nav${menuOpen ? ' site-nav--open' : ''}`} id="main-navigation" aria-label="Navegação principal">
          <Link className="site-nav__link" to="/" onClick={() => setMenuOpen(false)}>Início</Link>
          <Link className="site-nav__link site-nav__link--active" to="/" onClick={() => setMenuOpen(false)}>Produtos</Link>
          <a className="site-nav__link" href="#catalog-categories" onClick={() => setMenuOpen(false)}>Categorias</a>
        </nav>
        <div className="site-actions">
          <Link className="header-action" to="/login" aria-label="Acessar minha conta">
            <UserRound size={17} strokeWidth={1.8} aria-hidden="true" /><span>Conta</span>
          </Link>
          <button className="header-action header-action--muted" type="button" aria-disabled="true" aria-label="Carrinho indisponível" title="Carrinho em breve">
            <ShoppingBag size={17} strokeWidth={1.8} aria-hidden="true" /><span>Carrinho</span>
            <small aria-hidden="true">0</small>
          </button>
          <button className="mobile-menu-toggle" type="button" aria-expanded={menuOpen} aria-controls="main-navigation" aria-label={menuOpen ? 'Fechar menu' : 'Abrir menu'} onClick={() => setMenuOpen((open) => !open)}>
            {menuOpen ? <X size={19} aria-hidden="true" /> : <Menu size={19} aria-hidden="true" />}
          </button>
        </div>
      </header>
      {children}
    </div>
  );
}