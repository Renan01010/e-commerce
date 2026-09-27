import { Link, Route, Routes } from 'react-router-dom';
import { CatalogPage } from './pages/CatalogPage';
import { ProductDetailPage } from './pages/ProductDetailPage';

export default function App() {
  return (
    <div className="app-frame">
      <header className="site-header">
        <Link className="brand-mark" to="/" aria-label="TechStore, página inicial">
          <span className="brand-mark__symbol">T</span><span>techstore</span>
        </Link>
        <span className="site-header__descriptor">TECNOLOGIA PARA O SEU DIA</span>
        <Link className="header-link" to="/">Explorar <span aria-hidden="true">↗</span></Link>
      </header>
      <Routes>
        <Route path="/" element={<CatalogPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
        <Route path="*" element={<main className="detail-state"><h1>Página não encontrada</h1><Link className="button button--outline" to="/">Ir ao catálogo</Link></main>} />
      </Routes>
    </div>
  );
}