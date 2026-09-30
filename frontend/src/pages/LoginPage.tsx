import { Headphones, Laptop, Monitor, ShoppingCart, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CreateAccountLink } from '../components/auth/CreateAccountLink';
import { LoginForm } from '../components/auth/LoginForm';
import { AuthServiceError, authService } from '../services/authService';
import { useAuthStore } from '../store/authStore';
import type { LoginCredentials } from '../types/auth';
import './LoginPage.css';

export function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleLogin(credentials: LoginCredentials) {
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      const response = await authService.login(credentials);
      setSession({
        accessToken: response.accessToken,
        tokenType: response.tokenType,
        expiresAt: Date.now() + response.expiresIn * 1000,
      });
      navigate('/', { replace: true });
    } catch (error) {
      setErrorMessage(error instanceof AuthServiceError
        ? error.message
        : 'Não foi possível conectar. Tente novamente.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="login-screen">
      <section className="login-visual" aria-label="TechStore, tecnologia para o seu dia">
        <div className="login-visual__shade" />
        <Link className="login-brand login-brand--visual" to="/" aria-label="TechStore, página inicial">
          <ShoppingCart aria-hidden="true" size={30} strokeWidth={2.4} />
          <span>Tech<span>Store</span></span>
        </Link>

        <div className="login-visual__copy">
          <p className="login-visual__eyebrow">TECNOLOGIA AO SEU ALCANCE</p>
          <h2>Os melhores produtos de tecnologia em um só lugar.</h2>
          <p className="login-visual__description">
            Notebooks, computadores, acessórios e muito mais para acompanhar suas ideias.
          </p>
        </div>

        <ul className="login-categories" aria-label="Categorias da loja">
          <li><Laptop aria-hidden="true" /><span>Notebooks</span></li>
          <li><Monitor aria-hidden="true" /><span>Computadores</span></li>
          <li><ShieldCheck aria-hidden="true" /><span>Acessórios</span></li>
        </ul>
      </section>

      <section className="login-panel" aria-label="Acesso à conta TechStore">
        <div className="login-panel__content">
          <Link className="login-brand login-brand--panel" to="/" aria-label="TechStore, página inicial">
            <ShoppingCart aria-hidden="true" size={28} strokeWidth={2.4} />
            <span>Tech<span>Store</span></span>
          </Link>
          <p className="login-panel__intro">Acesse sua conta</p>
          <LoginForm onSubmit={handleLogin} isSubmitting={isSubmitting} errorMessage={errorMessage} />
          <CreateAccountLink />
          <p className="login-panel__secure"><Headphones aria-hidden="true" size={15} /> Suporte TechStore</p>
        </div>
        <footer className="login-panel__footer">© 2026 TechStore</footer>
      </section>
    </main>
  );
}