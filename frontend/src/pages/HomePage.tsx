import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  ArrowRight, Boxes, Cpu, Gamepad2, HardDrive, Headphones, Laptop, Monitor, Search, SlidersHorizontal, type LucideIcon,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { ProductCard } from '../components/ProductCard';
import { useCatalogStore } from '../store/catalogStore';

const categoryIcons: LucideIcon[] = [
  Laptop, Gamepad2, Monitor, Headphones, Cpu, HardDrive, SlidersHorizontal, Boxes,
];

function iconForCategory(name: string, index: number) {
  const normalized = name.toLocaleLowerCase('pt-BR');
  if (normalized.includes('notebook') || normalized.includes('laptop')) return Laptop;
  if (normalized.includes('monitor') || normalized.includes('tela')) return Monitor;
  if (normalized.includes('gamer') || normalized.includes('game')) return Gamepad2;
  if (normalized.includes('perif')) return Headphones;
  if (normalized.includes('armazen') || normalized.includes('storage')) return HardDrive;
  if (normalized.includes('component')) return Cpu;
  return categoryIcons[index % categoryIcons.length];
}

const benefits = [
  { title: 'Explore com clareza', description: 'Informações de produto reunidas em um só lugar.' },
  { title: 'Encontre seu próximo upgrade', description: 'Navegue pelo catálogo no seu próprio ritmo.' },
  { title: 'Monte novas possibilidades', description: 'Descubra opções para diferentes espaços e setups.' },
];

const campaignTiles = [
  { className: 'home-campaign--display', eyebrow: 'MONITORES', title: 'Mais espaço para jogar e criar', art: 'display' },
  { className: 'home-campaign--setup', eyebrow: 'PC GAMER', title: 'Prepare seu próximo setup', art: 'setup' },
  { className: 'home-campaign--gear', eyebrow: 'ACESSÓRIOS', title: 'Complete seu espaço do seu jeito', art: 'gear' },
];

export function HomePage() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const recentProducts = useCatalogStore((state) => state.recentProducts);
  const recentProductsLoading = useCatalogStore((state) => state.recentProductsLoading);
  const recentProductsError = useCatalogStore((state) => state.recentProductsError);
  const categories = useCatalogStore((state) => state.categories);
  const categoriesStatus = useCatalogStore((state) => state.categoriesStatus);
  const categoriesError = useCatalogStore((state) => state.categoriesError);
  const loadRecentProducts = useCatalogStore((state) => state.loadRecentProducts);
  const loadCategories = useCatalogStore((state) => state.loadCategories);
  const orderedCategories = useMemo(
    () => [...categories].sort((left, right) => left.displayOrder - right.displayOrder),
    [categories],
  );

  useEffect(() => {
    void loadRecentProducts();
    void loadCategories();
  }, [loadRecentProducts, loadCategories]);

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const query = search.trim();
    navigate(query ? `/catalog?query=${encodeURIComponent(query)}` : '/catalog');
  }

  return (
    <main className="home-shell">
      <section className="home-hero" aria-labelledby="home-title">
        <div className="home-hero__copy">
          <p className="home-eyebrow">TECHSTORE / TECNOLOGIA PARA EXPLORAR</p>
          <h1 id="home-title">Tecnologia <span>sem limites.</span></h1>
          <p className="home-hero__subtitle">Encontre novas ideias para trabalhar, criar e aproveitar cada momento.</p>
          <Link className="home-hero__cta" to="/catalog">Explorar catálogo <ArrowRight size={18} aria-hidden="true" /></Link>
          <div className="home-hero__benefits" aria-label="Sobre a experiência TechStore">
            {benefits.map((benefit) => (
              <div className="home-benefit" key={benefit.title}>
                <span className="home-benefit__mark" aria-hidden="true"><span /></span>
                <p><strong>{benefit.title}</strong><span>{benefit.description}</span></p>
              </div>
            ))}
          </div>
        </div>
        <div className="home-hero__art" aria-hidden="true">
          <div className="home-orbit home-orbit--one" />
          <div className="home-orbit home-orbit--two" />
          <div className="home-art__glow" />
          <div className="home-art__monitor"><div className="home-art__screen"><span /></div><i /></div>
          <div className="home-art__tower"><span /><span /><span /></div>
          <div className="home-art__panel" />
          <div className="home-art__floor" />
          <div className="home-art__spark home-art__spark--one" />
          <div className="home-art__spark home-art__spark--two" />
        </div>
      </section>

      <form className="home-search" role="search" onSubmit={submitSearch}>
        <Search size={19} aria-hidden="true" />
        <label className="sr-only" htmlFor="home-product-search">Buscar produtos no catálogo</label>
        <input id="home-product-search" type="search" placeholder="Busque produtos, marcas ou descrições" value={search} onChange={(event) => setSearch(event.target.value)} />
        <button type="submit">Buscar</button>
      </form>

      <section className="home-categories" id="home-categories" aria-labelledby="home-categories-title">
        <div className="home-section-heading">
          <div><p className="home-eyebrow">DESCUBRA</p><h2 id="home-categories-title">Explore por categoria</h2></div>
          <Link to="/catalog">Ver catálogo <ArrowRight size={16} aria-hidden="true" /></Link>
        </div>
        {categoriesStatus === 'loading' && categories.length === 0 ? (
          <div className="home-category-grid" role="status" aria-label="Carregando categorias">
            {Array.from({ length: 6 }, (_, index) => <div className="home-category-skeleton" aria-hidden="true" key={index} />)}
          </div>
        ) : categoriesStatus === 'error' && categories.length === 0 ? (
          <div className="home-inline-error" role="alert">
            <span>{categoriesError ?? 'As categorias não puderam ser carregadas.'}</span>
            <button type="button" onClick={() => void loadCategories(true)}>Tentar novamente</button>
          </div>
        ) : orderedCategories.length ? (
          <div className="home-category-grid">
            {orderedCategories.map((category, index) => {
              const Icon = iconForCategory(category.name, index);
              return (
                <Link className="home-category-card" key={category.id} to={`/catalog?categoryId=${encodeURIComponent(category.id)}`}>
                  <span className="home-category-card__icon"><Icon size={31} aria-hidden="true" /></span>
                  <span>{category.name}</span>
                  <ArrowRight className="home-category-card__arrow" size={15} aria-hidden="true" />
                </Link>
              );
            })}
            <Link className="home-category-card home-category-card--all" to="/catalog">
              <span className="home-category-card__icon"><Boxes size={31} aria-hidden="true" /></span>
              <span>Ver catálogo</span>
              <ArrowRight className="home-category-card__arrow" size={15} aria-hidden="true" />
            </Link>
          </div>
        ) : (
          <div className="home-empty-inline"><p>Nenhuma categoria disponível no momento.</p><Link to="/catalog">Ver catálogo</Link></div>
        )}
      </section>

      <section className="home-products" aria-labelledby="home-products-title">
        <div className="home-section-heading">
          <div><p className="home-eyebrow">RECÉM-CHEGADOS AO CATÁLOGO</p><h2 id="home-products-title">Produtos em destaque</h2><p className="home-section-heading__subtitle">Uma seleção dos produtos mais recentes do catálogo.</p></div>
          <Link to="/catalog">Ver todos <ArrowRight size={16} aria-hidden="true" /></Link>
        </div>
        {recentProductsError ? (
          <div className="home-inline-error" role="alert">
            <span>{recentProductsError}</span>
            <button type="button" onClick={() => void loadRecentProducts(true)}>Tentar novamente</button>
          </div>
        ) : recentProductsLoading && recentProducts.length === 0 ? (
          <div className="product-grid home-product-grid" role="status" aria-label="Carregando produtos">
            {Array.from({ length: 4 }, (_, index) => <div className="home-product-skeleton" aria-hidden="true" key={index} />)}
          </div>
        ) : recentProducts.length ? (
          <div className="product-grid home-product-grid">
            {recentProducts.map((product) => <ProductCard key={product.id} product={product} categoryName={categories.find((category) => category.id === product.categoryId)?.name} />)}
          </div>
        ) : (
          <div className="home-empty-inline"><p>Nenhum produto foi encontrado no catálogo.</p><Link to="/catalog">Voltar ao catálogo</Link></div>
        )}
      </section>

      <section className="home-campaigns" aria-label="Inspiração para seu próximo espaço">
        {campaignTiles.map((tile) => (
          <article className={`home-campaign ${tile.className}`} key={tile.eyebrow}>
            <div className="home-campaign__copy"><p>{tile.eyebrow}</p><h2>{tile.title}</h2><Link to="/catalog">Explorar catálogo <ArrowRight size={15} aria-hidden="true" /></Link></div>
            <div className={`home-campaign__art home-campaign__art--${tile.art}`} aria-hidden="true"><span /><i /><b /></div>
          </article>
        ))}
      </section>

      <footer className="home-footer"><span>TECHSTORE</span><span>Tecnologia para descobrir, escolher e aproveitar.</span><Link to="/catalog">Explorar produtos <ArrowRight size={15} aria-hidden="true" /></Link></footer>
    </main>
  );
}
