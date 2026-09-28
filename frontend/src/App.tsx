import type { ReactNode } from 'react';
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
  return (
    <div className="app-frame">
      <header className="site-header">
        <Link className="brand-mark" to="/" aria-label="TechStore, página inicial">
          <span className="brand-mark__symbol">T</span><span>techstore</span>
        </Link>
        <span className="site-header__descriptor">TECNOLOGIA PARA O SEU DIA</span>
        <Link className="header-link" to="/">Explorar <span aria-hidden="true">↗</span></Link>
      </header>
      {children}
    </div>
  );
}