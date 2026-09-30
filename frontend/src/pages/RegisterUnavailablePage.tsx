import { Link } from 'react-router-dom';

export function RegisterUnavailablePage() {
  return (
    <main className="register-placeholder" aria-labelledby="register-placeholder-title">
      <Link className="register-placeholder__brand" to="/" aria-label="TechStore, página inicial">
        <span className="register-placeholder__brand-icon" aria-hidden="true">T</span>
        TechStore
      </Link>
      <section className="register-placeholder__content">
        <p className="login-eyebrow">TECHSTORE</p>
        <h1 id="register-placeholder-title">Cadastro indisponível no momento</h1>
        <p>A criação de conta estará disponível em breve.</p>
        <Link className="register-placeholder__back" to="/login">Voltar ao login</Link>
      </section>
    </main>
  );
}